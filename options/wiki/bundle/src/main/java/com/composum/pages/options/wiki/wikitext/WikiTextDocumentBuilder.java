package com.composum.pages.options.wiki.wikitext;

import com.composum.sling.core.util.LinkUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;

import java.io.Writer;

/**
 *
 */
public class WikiTextDocumentBuilder extends HtmlDocumentBuilder {

    protected SlingHttpServletRequest request;

    public WikiTextDocumentBuilder(SlingHttpServletRequest request, Writer writer) {
        super(writer);
        this.request = request;
    }

    @Override
    protected String makeUrlAbsolute(String url) {
        return LinkUtil.getUrl(request, url);
    }
}
