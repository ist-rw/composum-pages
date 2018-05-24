package com.composum.pages.options.wiki.wikitext;

import com.composum.pages.options.wiki.WikiHtmlRenderer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;

import java.io.Writer;

public class WikiTextLanguageRenderer implements WikiHtmlRenderer {

    MarkupLanguage language;

    public WikiTextLanguageRenderer(MarkupLanguage language) {
        this.language = language;
    }

    @Override
    public void renderMarkup(SlingHttpServletRequest request, String text, Writer writer) {
        HtmlDocumentBuilder builder = createHtmlDocumentBuilder(request, writer);
        MarkupParser markupParser = new MarkupParser(language);
        markupParser.setBuilder(builder);
        markupParser.parse(text);
    }

    protected HtmlDocumentBuilder createHtmlDocumentBuilder(SlingHttpServletRequest request, Writer writer) {
        HtmlDocumentBuilder builder = new WikiTextDocumentBuilder(request, writer);
        // avoid the <html> and <body> tags
        builder.setEmitAsDocument(false);
        return builder;
    }
}
