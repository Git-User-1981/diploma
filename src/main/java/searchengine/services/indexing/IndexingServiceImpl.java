package searchengine.services.indexing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.Config;
import searchengine.dto.BasicResponse;
import searchengine.exceptions.ApiCallException;
import searchengine.model.Site;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {
    private final List<ConfigSite> configSites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final Map<String, String> messages;

    public IndexingServiceImpl(
            Config config,
            SiteRepository siteRepository,
            PageRepository pageRepository,
            LemmaRepository lemmaRepository,
            IndexRepository indexRepository
    ) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.configSites = config.getSites();
        this.messages = config.getMessages();
        SiteParse.init(
            siteRepository,
            pageRepository,
            lemmaRepository,
            indexRepository,
            config.getUserAgent(),
            config.getReferrer(),
            config.getMessages()
        );
    }

    @Override
    public BasicResponse startIndexing() {
        if (SiteParse.isProgress()) {
            throw new ApiCallException(messages.get("indexing_launched"));
        }

        SiteParse.setType(ParseType.FULL);
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        pool.submit(() -> {
            final List<SiteParse> threads = new ArrayList<>();
            for (ConfigSite configSite : configSites) {
                siteRepository.findByUrl(configSite.getUrl()).ifPresent(site -> {
                    log.info(messages.get("indexing_clear_site") + ": " + site.getUrl());
                    siteRepository.delete(site);
                });

                Site site = new Site();
                site.setUrl(configSite.getUrl());
                site.setName(configSite.getName());
                site.setStatus(StatusType.INDEXING);
                site.setStatusTime(new Date());
                siteRepository.save(site);

                threads.add(new SiteParse(site.getUrl(), site, true));
            }
            ForkJoinTask.invokeAll(threads);
            SiteParse.setType(ParseType.READY);
        });
        pool.shutdown();

        return new BasicResponse();
    }

    @Override
    public BasicResponse startIndexingPage(String url) {
        if (url.isBlank()) {
            throw new ApiCallException(messages.get("url_empty"));
        }
        if (SiteParse.isProgress()) {
            throw new ApiCallException(messages.get("indexing_launched"));
        }

        Site site = null;
        for (ConfigSite configSite : configSites) {
            if (url.startsWith(configSite.getUrl())) {
                Optional<Site> siteOpt = siteRepository.findByUrl(configSite.getUrl());
                if (siteOpt.isEmpty()) {
                    site = new Site();
                    site.setUrl(configSite.getUrl());
                    site.setName(configSite.getName());
                }
                else {
                    site = siteOpt.get();
                    pageRepository.findByPathAndSite(
                        url.replaceFirst(site.getUrl(), "/"),
                        site
                    ).ifPresent(page -> {
                        lemmaRepository.decrementAndClearFrequency(page.getId());
                        pageRepository.delete(page);
                    });
                }
                break;
            }
        }

        if (site == null) {
            throw new ApiCallException(messages.get("url_out_of_range"));
        }

        site.setStatus(StatusType.INDEXING);
        site.setStatusTime(new Date());
        siteRepository.save(site);

        SiteParse.setType(ParseType.SINGLE);
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final Site finalSite = site;
        pool.submit(() -> {
            ForkJoinTask.invokeAll(new SiteParse(url, finalSite, true));
            SiteParse.setType(ParseType.READY);
        });
        pool.shutdown();

        return new BasicResponse();
    }

    @Override
    public BasicResponse stopIndexing() {
        if (!SiteParse.isProgress()) {
            throw new ApiCallException(messages.get("indexing_not_launched"));
        }
        SiteParse.setType(ParseType.STOPPING);
        return new BasicResponse();
    }
}
