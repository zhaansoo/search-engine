package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexResponse;
import searchengine.dto.index.SearchResultData;
import searchengine.dto.index.SearchResultDto;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class IndexServiceImpl implements IndexService {
    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
    private final List<Thread> threads = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    private static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                return host;
            }
        } catch (URISyntaxException e) {
            logger.error("Invalid URL: " + url);
        }
        return null;
    }

    private static String extractPath(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null) {
                return path.isEmpty() ? "/" : path;
            }
        } catch (URISyntaxException e) {
            logger.error("Invalid URL: " + url);
        }
        return null;
    }

    @Override
    public IndexResponse startIndexing() {
        if (siteRepository.existsByStatus(SiteStatus.INDEXING)) {
            return new IndexResponse(false, "Индексация уже запущена");
        }
        List<Site> sites = deleteAllAndSaveSites();
        int cores = Runtime.getRuntime().availableProcessors();
        sites.forEach(i -> executor.submit(() -> {
            try {
                RecursiveIndex index = new RecursiveIndex(i.getUrl(), "/", i);
                Site invoke = new ForkJoinPool().invoke(index);
            } catch (Exception ex) {
                logger.error("Error while indexing | {}", ex.getMessage());
                i.setStatus(SiteStatus.FAILED);
                i.setError(ex.getMessage());
            }
            i.setStatus(SiteStatus.INDEXED);
            siteRepository.save(i);
        }));
        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            sites.stream().peek(i -> {
                i.setStatus(SiteStatus.FAILED);
                i.setError(e.getMessage());
            });
        }
        return new IndexResponse(true, null);
    }

    @Override
    @Transactional
    public IndexResponse stopIndexing() {
        if (!siteRepository.existsByStatus(SiteStatus.INDEXING))
            return new IndexResponse(false, "Индексация не запущена");
        executor.shutdown();
        List<Site> byNameIn = siteRepository.findByNameInAndStatus(sitesList.getSites().stream().map(it -> it.getName()).toList(), SiteStatus.INDEXING);
        siteRepository.saveAll(byNameIn.stream().peek(i -> {
            i.setStatus(SiteStatus.FAILED);
            i.setError("«Индексация остановлена пользователем»");
        }).toList());
        return new IndexResponse(true, null);
    }

    @Override
    public IndexResponse indexPage(String url) {
        Site site = null;
        try {
            if (!url.startsWith("https://")) {
                url = "https://" + url;
            }
            String domain = extractDomain(url);
            if (domain == null)
                return new IndexResponse(false, "Invalid URL");

            var s = sitesList.getSites().stream().filter(i -> i.getUrl().contains(domain)).findFirst().orElse(null);

            if (s == null)
                return new IndexResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

            site = findAndSaveSite(s);
            String path = extractPath(url);
            deletePage(site, path);

            Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").timeout(5000).ignoreContentType(true).execute();
            Document parse = response.parse();
            if (parse == null) {
                return new IndexResponse(false, "Failed to fetch page content");
            }

            Page page = createPage(site, path, parse.toString(), response.statusCode());
            if (response.statusCode() == 200) {
                processPageContent(page, site, parse.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            site.setError(e.getMessage());
            site.setStatus(SiteStatus.FAILED);
            logger.error(e.getMessage());
            return new IndexResponse(false, e.getMessage());
        } finally {
            if (site != null) {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(SiteStatus.INDEXED);
                siteRepository.save(site);
            }
        }
        return new IndexResponse(true, null);
    }

    @Override
    public SearchResultDto search(String query, String site, Integer offset, Integer limit)  {

        List<Site> sites = new ArrayList<>();
        if (site == null)
            sites.addAll(siteRepository.findAll());
        else {
            var siteConfig = sitesList.getSites().stream().filter(i -> i.getUrl().equalsIgnoreCase(site)).findFirst().get();
            sites.add(siteRepository.findByName(siteConfig.getName()).get());
        }
        if(!sites.stream().allMatch(i -> i.getStatus() == SiteStatus.INDEXED))
            return new SearchResultDto(false, "Sites not Indexed", null, null);

        Set<String> lemmaSet = null;
        try {
            lemmaSet = LemmaUtils.getInstance().getLemmaSet(query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (lemmaSet.isEmpty())
            return new SearchResultDto(false, "Query is Empty", null, null);
        Integer maxFrequencyBySiteIn = lemmaRepository.findMaxFrequencyBySiteIn(sites);
        int maxFrequency = (int) (maxFrequencyBySiteIn * 0.9);
        List<Lemma> lemmas = lemmaRepository.findBySiteInAndLemmaInAndFrequencyLessThanOrderByFrequencyAsc(sites, lemmaSet.stream().toList(), maxFrequency);

        if (lemmas.isEmpty())
            return new SearchResultDto(true, null, 0, List.of());

        List<Index> indices = indexRepository.findByLemma(lemmas.get(0));
        List<Page> pages = indices.stream().map(Index::getPage).toList();
        for (int i = 1; i < lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            indices = indexRepository.findByLemmaAndPageIn(lemma, pages);
            if (indices.isEmpty())
                return new SearchResultDto(true, null, 0, List.of());
        }

        Map<Page, Double> relative = indices.stream().collect(Collectors.toMap(Index::getPage, Index::getRank, Double::sum));
        Double maxValue = relative.values().stream().max(Double::compare).orElse(null);
        List<SearchResultData> absolute = relative.entrySet().stream().peek(i -> i.setValue(i.getValue() / maxValue)).sorted(Map.Entry.comparingByValue()).skip(offset).limit(limit)
                .map(i -> createSearchResult(i.getKey(), i.getValue(), query)).collect(Collectors.toList());


        SearchResultDto resultDto = new SearchResultDto();
        resultDto.setResult(true);
        resultDto.setCount(absolute.size());
        resultDto.setData(absolute);
        return resultDto;
    }

      private SearchResultData createSearchResult(Page page, double relevance, String query)  {
          SearchResultData resultDto = new SearchResultData();
          resultDto.setRelevance(relevance);
          resultDto.setSite(page.getSite().getUrl());
          resultDto.setSiteName(page.getSite().getName());
          resultDto.setUri(page.getPath());
          Document parse = Jsoup.parse(page.getContent());
          String title = parse.title();
          resultDto.setTitle(title);
          resultDto.setSnippet(extractSnippet(parse, query));
          return resultDto;
      }

    private String extractSnippet(Document parse, String query)  {
        String text = parse.text();

        List<String> snippets = new ArrayList<>();
        HashMap<String, String> textMap = null;
         Set<String> queryLemmaSet = null;
        try {
            textMap = LemmaUtils.getInstance().wordsToLemmas(text);
            queryLemmaSet = LemmaUtils.getInstance().getLemmaSet(query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> namesInContent = textMap.keySet().stream().filter(queryLemmaSet::contains).map(textMap::get).distinct().toList();
        if (namesInContent.isEmpty())
            return "NOT FOUND";


        int maxSize=300;
        int eachSize = maxSize / namesInContent.size();
        int[][] ints = new int[namesInContent.size()][2];
        int index = 0;
        for (String str: namesInContent) {
            int i = text.toLowerCase().indexOf(str);
            if (isInsideIndex(ints, i))
                continue;
            int start = Math.max(text.lastIndexOf('.', i - 1) + 1, i - eachSize / 2);
            int end = Math.min(text.indexOf('.', i + 1), i + eachSize /2);
            String htmlText = text.substring(i, i + str.length());
            snippets.add(text.substring(start, end).replaceAll("(?i)\\b" + htmlText + "\\b", "<b>$0</b>"));
            ints[index][0] = start;
            ints[index][1] = end;
            index++;
        }
        return String.join("...", snippets);
    }

    private boolean isInsideIndex(int[][] ints, int i) {
        for (int[] anInt : ints) {
            int start = anInt[0];
            int end = anInt[1];
            if (start <= i && i <= end)
                return true;
        }
        return false;
    }

    private String highlightMatches(String snippet, List<String> matchingWords) {
        for (String word : matchingWords) {
            snippet = snippet.replaceAll("(?i)\\b" + word + "\\b", "<b>$0</b>");
        }
        return snippet;
    }


    @Transactional
    private void deletePage(Site site, String path) {
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
            if (!lemmaDeleteArrayList.isEmpty())
                lemmaRepository.deleteAll(lemmaDeleteArrayList);
            if (!lemmaArrayList.isEmpty())
                lemmaRepository.saveAll(lemmaArrayList);
            pageRepository.delete(page);
            logger.info("Page with ID {} deleted successfully.", page.getId());

        }
    }

    @Transactional
    private List<Site> deleteAllAndSaveSites() {

//        List<String> sitesName = sitesList.getSites().stream().map(it -> it.getName()).toList();
//        List<Site> sites = siteRepository.findByNameIn(sitesName);
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        List<Site> list = sitesList.getSites().stream().map(this::createSite).toList();
        return siteRepository.saveAll(list);
    }

    @Transactional
    private void processPageContent(Page page, Site site, String content) throws IOException {
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
    private Site findAndSaveSite(searchengine.config.Site s) {
        Site site = siteRepository.findByName(s.getName()).orElse(null);
        if (site == null) {
            site = createSite(s);
        } else {
            site.setStatus(SiteStatus.INDEXING);
        }
        siteRepository.save(site);
        return site;
    }

    private Site createSite(searchengine.config.Site configSite) {
        Site site = new Site();
        site.setName(configSite.getName());
        site.setUrl(configSite.getUrl());
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
//        site.setPages(new ArrayList<>());
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

    @Transactional
    private Page createPage(Site site, String path, String content, int statusCode) {
        Page page = new Page();
        page.setSite(site);
        page.setContent(content);
        page.setCode(statusCode);
        page.setPath(path);
        return pageRepository.save(page);
    }


    private class RecursiveIndex extends RecursiveTask<Site> {

        private final String url;
        private final String path;
        private final Site site;

        public RecursiveIndex(String url, String path, Site site) {
            this.url = url;
            this.path = path;
            this.site = site;
        }

        @Override
        protected Site compute() {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(500, 3001));
//                 Jsoup.connect("https://www.facebook.com/").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com");
                Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").timeout(5000).ignoreContentType(true).execute();
                Document parse = response.parse();
                if (parse == null) {
                    Thread.currentThread().interrupt();
                }


                Elements elements = parse.select("a[href]");
                for (Element element : elements) {
                    String subUrl = element.absUrl("href");
                    String subPath = element.attr("href");
                    if (!subUrl.isEmpty() && subUrl.startsWith(url) && !subUrl.contains("#")
                            && !pageRepository.existsByPath(subPath)) {
                        Page page = createPage(site, subPath, parse.toString(), response.statusCode());
                        updateSite();
                        if (response.statusCode() == 200) {
                            processPageContent(page, site, parse.toString());
                        }
                        RecursiveIndex index = new RecursiveIndex(subUrl, subPath, site);
                        index.invoke();
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
//                Thread.currentThread().interrupt();
            }

            return site;
        }

        @Transactional
        private void updateSite() {
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }


    }
}
