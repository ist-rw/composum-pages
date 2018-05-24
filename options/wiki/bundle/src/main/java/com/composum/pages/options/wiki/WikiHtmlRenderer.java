package com.composum.pages.options.wiki;

import org.apache.sling.api.SlingHttpServletRequest;

import java.io.Writer;

/**
 * Created by rw on 24.08.15.
 */
public interface WikiHtmlRenderer {

    void renderMarkup(SlingHttpServletRequest request, String text, Writer writer);
}
