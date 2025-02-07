package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.IndexResponse;
import searchengine.dto.index.SearchResultDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    @Autowired
    private IndexService indexService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<IndexResponse> startIndexing() {
        return ResponseEntity.ok(indexService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexResponse> stopIndexing() {
        return ResponseEntity.ok(indexService.stopIndexing());
    }

    @PostMapping("indexPage")
    public ResponseEntity<IndexResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResultDto> statistics(@RequestParam(name = "query") String query,
                                                      @RequestParam(name = "site", required = false) String site,
                                                      @RequestParam(name = "offset", defaultValue = "0") Integer offset,
                                                      @RequestParam(name = "limit", defaultValue = "20") Integer limit) {
        var resultDto = indexService.search(query, site, offset, limit);
        if(!resultDto.isResult())
            return ResponseEntity.badRequest().body(resultDto);
        return ResponseEntity.ok(resultDto);
    }
}
