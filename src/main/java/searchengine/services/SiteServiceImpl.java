package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.shared.SiteStatus;
import searchengine.utility.LemmaUtils;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private static final Logger logger = LoggerFactory.getLogger(SiteServiceImpl.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional
    @Override
    public void updateSite(Site site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }


    @Transactional
    @Override
    public Page createPage(Site site, String path, String content, int statusCode) {
        Page page = new Page();
        page.setSite(site);
        page.setContent(content);
        page.setCode(statusCode);
        page.setPath(path);
        return pageRepository.save(page);
    }


    @Transactional
    @Override
    public Site findAndSaveSite(searchengine.config.Site s) {
        Site site = siteRepository.findByName(s.getName()).orElse(null);
        if (site == null) {
            site = createSite(s);
        } else {
            site.setStatus(SiteStatus.INDEXING);
        }
        siteRepository.save(site);
        return site;
    }


    @Transactional
    @Override
    public void processPageContent(Page page, Site site, String content) throws IOException {
        Map<String, Integer> lemmaMap = LemmaUtils.getInstance().collectLemmas(content);
        List<Lemma> byLemmaInAndSite = lemmaRepository.findByLemmaInAndSite(lemmaMap.keySet().stream().toList(), site);
        List<Lemma> listLemma = new ArrayList<>(byLemmaInAndSite.stream().peek(i -> i.setFrequency(i.getFrequency() + 1)).toList());
        List<String> list = byLemmaInAndSite.stream().map(i -> i.getLemma()).toList();
        listLemma.addAll(lemmaMap.keySet().stream().filter(i -> !list.contains(i)).map(i -> createLemma(site, i)).toList());
        lemmaRepository.saveAll(listLemma);
        List<Index> listIndex = listLemma.stream().map(i -> createIndex(page, i, Double.valueOf(lemmaMap.get(i.getLemma())))).toList();
        indexRepository.saveAll(listIndex);
    }

    @Transactional
    @Override
    public List<Site> deleteAllAndSaveSites() {
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        List<Site> list = sitesList.getSites().stream().map(this::createSite).toList();
        return siteRepository.saveAll(list);
    }

    @Transactional
    @Override
    public void deletePage(Site site, String path) {
        Page page = pageRepository.findBySiteAndPath(site, path).orElse(null);

        if (page != null) {
            List<Index> indices = indexRepository.findByPage(page);
            indexRepository.deleteAll(indices);
            List<Lemma> lemmaArrayList = new ArrayList<>();
            List<Lemma> lemmaDeleteArrayList = new ArrayList<>();
            for (Index index : indices) {
                Lemma lemma = index.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);

                if (lemma.getFrequency() <= 0) {
                    lemmaDeleteArrayList.add(lemma);
                } else {
                    lemmaArrayList.add(lemma);
                }
            }
            if (!lemmaDeleteArrayList.isEmpty()) lemmaRepository.deleteAll(lemmaDeleteArrayList);
            if (!lemmaArrayList.isEmpty()) lemmaRepository.saveAll(lemmaArrayList);
            pageRepository.delete(page);
            logger.info("Page with ID {} deleted successfully.", page.getId());
        }
    }

    private Site createSite(searchengine.config.Site configSite) {
        Site site = new Site();
        site.setName(configSite.getName());
        site.setUrl(configSite.getUrl());
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        return site;
    }

    private Lemma createLemma(Site site, String text) {
        Lemma lemma = new Lemma();
        lemma.setLemma(text);
        lemma.setSite(site);
        lemma.setFrequency(1);
        return lemma;
    }

    private Index createIndex(Page page, Lemma lemma, Double rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }
}
