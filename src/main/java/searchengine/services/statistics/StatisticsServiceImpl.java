package searchengine.services.statistics;

import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.config.ConfigSite;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.SiteParse;

import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final List<ConfigSite> configSites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public StatisticsServiceImpl(
            Config config,
            SiteRepository siteRepository,
            PageRepository pageRepository,
            LemmaRepository lemmaRepository
    ) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.configSites = config.getSites();
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(configSites.size());
        total.setIndexing(SiteParse.isProgress());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (ConfigSite configSite : configSites) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(configSite.getName());
            item.setUrl(configSite.getUrl());

            Optional<Site> siteOpt = siteRepository.findByUrl(configSite.getUrl());
            if (siteOpt.isPresent()) {
                Site site = siteOpt.get();

                int pagesCount = pageRepository.countAllBySite(site);
                int lemmasCount = lemmaRepository.countAllBySite(site);

                item.setPages(pagesCount);
                item.setLemmas(lemmasCount);
                item.setStatus(site.getStatus().name());
                item.setStatusTime(site.getStatusTime().getTime());
                item.setError(site.getLastError());
                total.setPages(total.getPages() + pagesCount);
                total.setLemmas(total.getLemmas() + lemmasCount);
            }
            else {
                item.setPages(0);
                item.setLemmas(0);
                item.setStatus("");
                item.setStatusTime(new Date().getTime());
            }

            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
