package com.composum.pages.options.wiki.pegdown;

import com.composum.pages.options.wiki.WikiHtmlRenderer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

public class PegDownHtmlRenderer implements WikiHtmlRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(PegDownHtmlRenderer.class);

    protected String renderMarkup(SlingHttpServletRequest request, String text) {
        LinkRenderer linkRenderer = new LinkRenderer();
        PegDownProcessor processor = new PegDownProcessor();
        String htmlText = processor.markdownToHtml(text, linkRenderer);
        return htmlText;
    }

    @Override
    public void renderMarkup(SlingHttpServletRequest request, String text, Writer writer) {
        try {
            String htmlText = renderMarkup(request, text);
            writer.append(htmlText);
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
    }
}
