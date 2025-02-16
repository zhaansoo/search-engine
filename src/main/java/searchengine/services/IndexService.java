package searchengine.services;

import searchengine.dto.index.IndexResponse;
import searchengine.dto.index.SearchResultDto;

public interface IndexService {
    IndexResponse startIndexing();
    IndexResponse stopIndexing();
    IndexResponse indexPage(String url);
    SearchResultDto search(String query, String site, Integer offset, Integer limit) ;
}
