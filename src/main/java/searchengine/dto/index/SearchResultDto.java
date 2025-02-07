package searchengine.dto.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private boolean result;
    private String error;
    private Integer count;
    private List<SearchResultData> data;
}
