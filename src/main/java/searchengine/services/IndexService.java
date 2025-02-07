package searchengine.services;

import searchengine.dto.index.IndexResponse;
import searchengine.dto.index.SearchResultDto;

import java.io.IOException;

public interface IndexService {
    public IndexResponse startIndexing();
    public IndexResponse stopIndexing();
    public IndexResponse indexPage(String url);

    SearchResultDto search(String query, String site, Integer offset, Integer limit) ;
}
