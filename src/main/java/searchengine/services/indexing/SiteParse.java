package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.morphology.MorphologyAnalyzer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Slf4j
@RequiredArgsConstructor
public class SiteParse extends RecursiveAction {
    @Getter
    private String url;
    private boolean isRoot;
    private final transient Site site;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static String userAgent;
    private static String referrer;
    private static Map<String, String> messages;
    private List<String> requestedUrlQueue;

    @Setter
    @Getter
    private static volatile ParseType type = ParseType.READY;

    public static void init(
        SiteRepository siteRepository,
        PageRepository pageRepository,
        LemmaRepository lemmaRepository,
        IndexRepository indexRepository,
        String userAgent,
        String referrer,
        Map<String, String> messages
    ) {
        SiteParse.pageRepository = pageRepository;
        SiteParse.siteRepository = siteRepository;
        SiteParse.lemmaRepository = lemmaRepository;
        SiteParse.indexRepository = indexRepository;
        SiteParse.userAgent = userAgent;
        SiteParse.referrer = referrer;
        SiteParse.messages = messages;
    }

    public SiteParse(String url, Site site, boolean isRoot) {
        this.url = url;
        this.site = site;
        this.isRoot = isRoot;
        if (isRoot) {
            this.requestedUrlQueue =  new CopyOnWriteArrayList<>();
        }
    }

    public SiteParse(String url, Site site, List<String> requestedUrlQueue) {
        this(url, site, false);
        this.requestedUrlQueue = requestedUrlQueue;
    }

    @Override
    protected void compute() {
        if (isProgress() && type != ParseType.STOPPING) {
            try {
                if (!isRoot) {
                    sleepRandom();
                }

                if (requestedUrlQueue.contains(url) || urlExists(url)) {
                    return;
                }

                requestedUrlQueue.add(url);
                log.info(messages.get(isRoot ? "indexing_start" : "indexing_processing") + ": " + url);

                Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(15000)
                    .followRedirects(true)
                    .execute();

                parseResponse(response);

                if (isRoot) {
                    if (type == ParseType.STOPPING) {
                        userInterruptIndexing();
                    }
                    else {
                        site.setStatus(StatusType.INDEXED);
                        log.info(messages.get("indexing_complete") + ": " + url);
                    }
                    requestedUrlQueue.clear();
                }
                else {
                    requestedUrlQueue.remove(url);
                }
            }
            catch (UnknownHostException e) {
                saveError(500, messages.get("indexing_unknown_url"));
            }
            catch (HttpStatusException e) {
                saveError(e.getStatusCode(), e.getMessage());
            }
            catch (SocketTimeoutException e) {
                saveError(504, messages.get("indexing_read_timeout_error"));
            }
            catch (Exception e) {
                String msgKey = e instanceof IOException ? "indexing_url_parse_error" : "indexing_unexpected_error";
                saveError(
                    500,
                    messages.get(msgKey) + ": " + e.getMessage(),
                    Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining(System.lineSeparator()))
                );
            }
        }
        else if (type == ParseType.STOPPING && isRoot) {
            userInterruptIndexing();
        }

        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    private void sleepRandom() {
        try {
            sleep( (long) (Math.random() * 50) + 100);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String clearURI(String url) {
        return url.replaceAll("\\?.+","")
                  .replaceAll("#.+", "");
    }

    private boolean urlExists(String url) {
        return pageRepository.existsByPathAndSite(clearHost(url), site);
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + site.getUrl());
        Pattern patternFile = Pattern.compile("(\\S+(\\.(?i)(eps|zip|jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|mp4))$)");
        Pattern patternAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return (patternRoot.matcher(url).find()
            && !patternFile.matcher(url).find()
            && !patternAnchor.matcher(url).find()
            && !urlExists(url));
    }

    private void parseResponse(Connection.Response response) throws IOException {
        if (response.statusCode() == 200) {
            String contentType = response.contentType();
            if (contentType != null && contentType.startsWith("text/html")) {
                Page page = pageAdd(response.statusCode(), response.body());
                createPageIndex(page);

                if (type == ParseType.FULL) {
                    final List<SiteParse> children = new ArrayList<>();
                    response.parse().select("body a[href]").forEach(a -> {
                        String childUrl = clearURI(a.absUrl("href"));
                        if (isCorrectUrl(childUrl)) {
                            children.add(new SiteParse(childUrl, site, requestedUrlQueue));
                        }
                    });
                    ForkJoinTask.invokeAll(children);
                }
            }
            else {
                saveError(500, messages.get("indexing_wrong_content_type") + ": " + contentType);
            }
        }
        else {
            pageAdd(response.statusCode(), response.statusMessage());
        }
    }

    private synchronized Page pageAdd(Integer statusCode, String content) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(clearHost(url));
        page.setCode(statusCode);
        page.setContent(content);
        pageRepository.saveAndFlush(page);

        return page;
    }

    private void createPageIndex(Page page) throws IOException {
        String text = Jsoup.parse(page.getContent()).select("title,body").text();
        Map<String, Integer> lemmas = MorphologyAnalyzer.getInstance().getLemmaListWithCount(text);

        final Set<Lemma> lemmasList = new HashSet<>();
        final Set<Index> indicesList = new HashSet<>();
        lemmas.forEach((lemmaStr, rank) -> {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaStr);
            lemma.setFrequency(1);
            lemmasList.add(lemma);

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(rank);
            indicesList.add(index);
        });

        synchronized (SiteParse.class) {
            lemmaRepository.saveAllAndFlush(lemmasList);
            indexRepository.saveAllAndFlush(indicesList);
        }
    }

    private String clearHost(String url) {
        return url.replaceFirst(site.getUrl(), "/");
    }

    public static boolean isProgress() {
        return type != ParseType.READY;
    }

    private void saveError(Integer errorCode, String... error){
        String errorText = error[0];

        if (isRoot) {
            site.setStatus(StatusType.FAILED);
            site.setLastError(errorText);
        }

        if (error.length > 1) {
            errorText = error[0] + System.lineSeparator() + error[1];
        }

        pageAdd(errorCode, errorText);

        log.error(url + " " + errorCode + " " + errorText);
    }

    private void userInterruptIndexing() {
        site.setStatus(StatusType.FAILED);
        site.setLastError(messages.get("indexing_stopped_by_user"));
        log.warn(messages.get("indexing_interrupt") + ": " + url);
    }
}
