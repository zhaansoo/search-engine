package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.List;

public interface SiteService {
    void deletePage(Site site, String path);

    List<Site> deleteAllAndSaveSites();

    void processPageContent(Page page, Site site, String content) throws IOException;

    Site findAndSaveSite(searchengine.config.Site s);

    Page createPage(Site site, String path, String content, int statusCode);

    void updateSite(Site site);
}

