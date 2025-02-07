package searchengine.dto.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double relevance;
}
