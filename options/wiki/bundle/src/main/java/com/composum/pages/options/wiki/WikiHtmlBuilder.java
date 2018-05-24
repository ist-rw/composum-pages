package com.composum.pages.options.wiki;

import org.apache.sling.api.SlingHttpServletRequest;

import java.io.Writer;

/**
 * Created by rw on 24.08.15.
 */
public interface WikiHtmlBuilder {

    String CONFLUENCE = "confluence";
    String MARKDOWN = "markdown";
    String MEDIAWIKI = "mediawiki";
    String TEXTILE = "textile";
    String TRACWIKI = "tracwiki";
    String TWIKI = "twiki";

    String buildMarkup(SlingHttpServletRequest request, String text, String language);

    void buildMarkup(SlingHttpServletRequest request, String text, String language, Writer writer);
}
