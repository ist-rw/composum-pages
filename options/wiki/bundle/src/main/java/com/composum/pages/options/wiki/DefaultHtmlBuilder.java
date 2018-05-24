package com.composum.pages.options.wiki;

import com.composum.pages.options.wiki.wikitext.WikiTextLanguageRenderer;
import com.composum.pages.options.wiki.pegdown.PegDownHtmlRenderer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.mediawiki.core.MediaWikiLanguage;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;
import org.eclipse.mylyn.wikitext.tracwiki.core.TracWikiLanguage;
import org.eclipse.mylyn.wikitext.twiki.core.TWikiLanguage;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Component(
        label = "Composum Wiki Markup Builder",
        description = "Provides the transformation of Wiki text content to HTML markup text.",
        immediate = true,
        metatype = false
)
@Service
public class DefaultHtmlBuilder implements WikiHtmlBuilder {

    public final Map<String, WikiHtmlRenderer> languageMap;

    public DefaultHtmlBuilder() {
        languageMap = new HashMap<>();
        languageMap.put(CONFLUENCE, new WikiTextLanguageRenderer(new ConfluenceLanguage()));
        languageMap.put(MARKDOWN, new PegDownHtmlRenderer());
        languageMap.put(MEDIAWIKI, new WikiTextLanguageRenderer(new MediaWikiLanguage()));
        languageMap.put(TEXTILE, new WikiTextLanguageRenderer(new TextileLanguage()));
        languageMap.put(TRACWIKI, new WikiTextLanguageRenderer(new TracWikiLanguage()));
        languageMap.put(TWIKI, new WikiTextLanguageRenderer(new TWikiLanguage()));
    }

    public String buildMarkup(SlingHttpServletRequest request, String text, String language) {
        StringWriter writer = new StringWriter();
        buildMarkup(request, text, language, writer);
        return writer.toString();
    }

    public void buildMarkup(SlingHttpServletRequest request, String text, String language, Writer writer) {
        WikiHtmlRenderer renderer = languageMap.get(language.toLowerCase());
        renderer.renderMarkup(request, text, writer);
    }
}
