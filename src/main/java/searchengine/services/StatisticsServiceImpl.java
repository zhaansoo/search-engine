package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.shared.SiteStatus;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(siteRepository.existsByStatus(SiteStatus.INDEXING));

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
             Site site = sitesList.get(i);
            var siteModel = siteRepository.findByName(site.getName()).get();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteModel.getName());
            item.setUrl(siteModel.getUrl());
            List<Page> bySite = pageRepository.findBySite(siteModel);
            int pages = pageRepository.findBySite(siteModel).size();
            List<Lemma> bySite1 = lemmaRepository.findBySite(siteModel);
            int lemmas = pages * bySite1.size();

            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteModel.getStatus().toString());
            item.setError(siteModel.getError());
            item.setStatusTime(siteModel.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
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
