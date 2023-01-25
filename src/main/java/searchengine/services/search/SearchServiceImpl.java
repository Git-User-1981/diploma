package searchengine.services.search;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.config.ConfigSite;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.ApiCallException;
import searchengine.model.Index;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.morphology.MorphologyAnalyzer;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    private final List<ConfigSite> configSites;
    private final Map<String, String> messages;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;

    public SearchServiceImpl(
            Config config,
            LemmaRepository lemmaRepository,
            SiteRepository siteRepository,
            IndexRepository indexRepository
    ) {
        this.messages = config.getMessages();
        this.configSites = config.getSites();
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
    }

    private boolean checkSiteInConfig(String checkedSiteUrl) {
        for (ConfigSite configSite: configSites) {
            if (configSite.getUrl().equals(checkedSiteUrl)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SearchResponse search(SearchRequest req) {

        if (!req.getSite().isBlank() && !checkSiteInConfig(req.getSite())) {
            throw new ApiCallException(messages.get("search_site_not_found"));
        }

        List<String> originalLemmas;
        try {
            originalLemmas = MorphologyAnalyzer.getInstance().getLemmaList(req.getQuery());
        }
        catch (IOException e) {
            String errorText = e.getMessage() + System.lineSeparator() +
                    Arrays.stream(e.getStackTrace())
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining(System.lineSeparator()));
            log.error("Ошибка при получении лемм: " + System.lineSeparator() + errorText);
            throw new ApiCallException(messages.get("search_unexpected_error"));
        }

        final List<Integer> sitesId = getListSiteIdByUrl(req.getSite());

        List<String> filteredLemmas = lemmaRepository.filterLemmasOverLimit(
            originalLemmas,
            sitesId,
            originalLemmas.size() > 1 ? 0.6 : 1
        );

        SearchResponse searchResponse = new SearchResponse();

        if (!filteredLemmas.isEmpty()) {
            List<Index> indices = new ArrayList<>();
            for(String lemma : filteredLemmas) {
                if (indices.isEmpty()) {
                    indices = indexRepository.findAllByLemmaAndSitesId(lemma, sitesId);
                }
                else {
                    List<Integer> pagesId = indices.stream().map(Index::getPageId).toList();
                    indices = indicesMerge(
                        indexRepository.findAllByLemmaAndSitesIdAndPagesId(lemma, sitesId, pagesId),
                        indices
                    );
                }
            }

            float relMax = indices.stream().max(Comparator.comparing(Index::getRank)).orElse(new Index()).getRank();
            indices.forEach(index -> index.setRank(index.getRank() / relMax));
            indices.sort((o1, o2) -> Double.compare(o2.getRank(), o1.getRank()));

            searchResponse.setCount(indices.size());
            searchResponse.setData(buildResult(indices, req.getOffset(), req.getLimit(), originalLemmas));
        }

        return searchResponse;
    }

    private List<Integer> getListSiteIdByUrl(String siteUrl) {
        final List<String> sitesUrl = new ArrayList<>();
        if (siteUrl.isBlank()) {
            for (ConfigSite configSite: configSites) {
                sitesUrl.add(configSite.getUrl());
            }
        }
        else {
            sitesUrl.add(siteUrl);
        }
        return siteRepository.findAllIdByUrls(sitesUrl);
    }

    private List<Index> indicesMerge(List<Index> indicesNew, List<Index> indicesOld) {
        for (Index indexNew : indicesNew) {
            for (Index indexOld : indicesOld) {
                if (indexNew.getPageId().equals(indexOld.getPageId())) {
                    indexNew.setRank(indexNew.getRank() + indexOld.getRank());
                }
            }
        }
        return indicesNew;
    }

    private List<SearchData> buildResult(List<Index> indices, int offset, int limit, List<String> lemmas) {
        List<SearchData> resultData = new ArrayList<>();

        if (offset < indices.size()) {
            int offsetLimit = offset + limit - 1;
            for (int idx = offset; idx < indices.size(); idx++) {
                if (idx > offsetLimit) {
                    break;
                }

                Index index = indices.get(idx);
                SearchData searchData = new SearchData();
                searchData.setSite(index.getPage().getSite().getUrl().replaceAll("/$", ""));
                searchData.setSiteName(index.getPage().getSite().getName());
                searchData.setUri(index.getPage().getPath());
                searchData.setRelevance(index.getRank());

                Document pageContent = Jsoup.parse(index.getPage().getContent());

                searchData.setTitle(pageContent.title());
                searchData.setSnippet(createSnippet(pageContent.select("body").text(), lemmas));

                resultData.add(searchData);
            }
        }

        return resultData;
    }

    private String createSnippet(String text, List<String> searchLemmas) {
        String snippet = "";
        try {
            for (String word : textToWordsArray(text)) {
                if (wordExists(word, searchLemmas)) {
                    int position = wholeWordPosition(word, text);
                    int startIndex = text.lastIndexOf(". ", position);
                    startIndex = startIndex == -1 ? 0 : startIndex + 2;
                    int endIndex = startIndex + 200;

                    if (endIndex < position + word.length()) {
                        startIndex = position - 100;
                        startIndex = Math.max(startIndex, 0);
                        endIndex = position + 100;
                        snippet = "...";
                    }

                    endIndex = Math.min(endIndex, text.length());

                    snippet += text.substring(startIndex, endIndex);
                    for (String snippetWord : textToWordsArray(snippet)) {
                        if (wordExists(snippetWord, searchLemmas)) {
                            snippet = snippet.replaceAll(snippetWord, "<b>" + snippetWord + "</b>");
                        }
                    }
                    snippet += "...";
                    break;
                }
            }
        }
        catch (IOException e) {
            String errorText = e.getMessage() + System.lineSeparator() +
                Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining(System.lineSeparator()));
            log.error("Ошибка при получении лемм для превью: " + System.lineSeparator() + errorText);
            throw new ApiCallException(messages.get("search_unexpected_error"));
        }
        return snippet;
    }

    private boolean wordExists(String word, List<String> searchLemmas) throws IOException {
        boolean result = false;
        MorphologyAnalyzer morphInstance = MorphologyAnalyzer.getInstance();
        List<String> wordLemmas = morphInstance.wordToLemma(word.toLowerCase());
        for (String wordLemma : wordLemmas) {
            if (wordLemma != null && searchLemmas.contains(wordLemma)) {
                return true;
            }
        }
        return result;
    }

    private String[] textToWordsArray(String text) {
        return text.replaceAll("[\\p{Punct}«»—:]+", "").trim().split("\\s+");
    }

    private int wholeWordPosition(String keyword, String text) {
        Matcher matcher = Pattern.compile("\\b" + keyword + "\\b").matcher(text);
        return matcher.find() ? matcher.start() : - 1;
    }
}
