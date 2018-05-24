package com.composum.pages.options.wiki.model;

import com.composum.pages.commons.model.Element;
import com.composum.pages.options.wiki.WikiHtmlBuilder;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import java.util.ArrayList;
import java.util.List;

public class Article extends Element {

    private static final Logger LOG = LoggerFactory.getLogger(Article.class);

    public static final String WIKI_SUBTITLE_PROP = "subtitle";

    public static final String WIKI_MARKUP_PROP = "markup";
    public static final String WIKI_REF_PROP = "reference";
    public static final String[] WIKI_FILE_NAMES = new String[]{
            "wikifile.txt", "wikifile.md", "wikifile"
    };
    public static final String WIKI_DEFAULT_ENCODING = "UTF-8";

    public static final String WIKI_TYPE_PROP = "type";
    public static final String WIKI_DEFAULT_TYPE = WikiHtmlBuilder.CONFLUENCE;
    public static final List<String> WIKI_TYPES;

    static {
        WIKI_TYPES = new ArrayList<>();
        WIKI_TYPES.add(WikiHtmlBuilder.MARKDOWN);
        WIKI_TYPES.add(WikiHtmlBuilder.CONFLUENCE);
        WIKI_TYPES.add(WikiHtmlBuilder.TEXTILE);
        WIKI_TYPES.add(WikiHtmlBuilder.MEDIAWIKI);
        WIKI_TYPES.add(WikiHtmlBuilder.TWIKI);
        WIKI_TYPES.add(WikiHtmlBuilder.TRACWIKI);
    }

    protected transient String subtitle;
    protected transient String markup;

    public String getSubtitle() {
        if (subtitle == null) {
            subtitle = getProperty(WIKI_SUBTITLE_PROP, getLocale(), "");
        }
        return subtitle;
    }

    public String getMarkup() {
        if (markup == null) {
            String type = getType();
            String code = getCode();
            WikiHtmlBuilder builder = context.getService(WikiHtmlBuilder.class);
            markup = builder.buildMarkup(context.getRequest(), code, type);
        }
        return markup;
    }

    public String getCode() {
        String code = getProperty(WIKI_MARKUP_PROP, getLocale(), "");
        if (StringUtils.isBlank(code)) {
            Resource content = null;
            String reference = getProperty(WIKI_REF_PROP, getLocale(), "");
            if (StringUtils.isNotBlank(reference)) {
                content = resolver.getResource(resource, reference);
            }
            if (content == null) {
                content = determineFile(resource, "");
            }
            if (content == null) {
                content = determineFile(resource, ResourceUtil.CONTENT_NODE + "/");
            }
            if (content == null) {
                content = resource;
            }
            Binary binary = ResourceUtil.getBinaryData(content);
            if (binary != null) {
                try {
                    code = IOUtils.toString(binary.getStream(), WIKI_DEFAULT_ENCODING);
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return code;
    }

    protected Resource determineFile(Resource resource, String path) {
        for (String name : WIKI_FILE_NAMES) {
            Resource file = resource.getChild(path + name);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public String getType() {
        return getProperty(WIKI_TYPE_PROP, null, WIKI_DEFAULT_TYPE);
    }

    public List<String> getTypes() {
        return WIKI_TYPES;
    }
}
