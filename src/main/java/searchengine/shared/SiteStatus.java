package searchengine.shared;

import lombok.AllArgsConstructor;

public enum SiteStatus {
    INDEXING("INDEXING"),
    INDEXED("INDEXED"),
    FAILED("FAILED");

    private String name;

    SiteStatus(String name) {
        this.name =name;
    }

    private boolean equalsName(String name) {
        return this.name.equalsIgnoreCase(name);
    }
}
