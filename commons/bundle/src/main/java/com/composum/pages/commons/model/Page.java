package com.composum.pages.commons.model;

import com.composum.pages.commons.model.properties.Language;
import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.service.PageManager;
import com.composum.platform.models.annotations.DetermineResourceStategy;
import com.composum.platform.models.annotations.PropertyDetermineResourceStrategy;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.platform.security.AccessMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.composum.pages.commons.PagesConstants.DEFAULT_EDIT_CATEGORY;
import static com.composum.pages.commons.PagesConstants.DEFAULT_VIEW_CATEGORY;
import static com.composum.pages.commons.PagesConstants.LANGUAGE_KEY;
import static com.composum.pages.commons.PagesConstants.NODE_TYPE_PAGE;
import static com.composum.pages.commons.PagesConstants.NODE_TYPE_PAGE_CONTENT;
import static com.composum.pages.commons.PagesConstants.PAGES_PREFIX;
import static com.composum.pages.commons.PagesConstants.PROP_EDIT_CATEGORY;
import static com.composum.pages.commons.PagesConstants.PROP_SLING_TARGET;
import static com.composum.pages.commons.PagesConstants.PROP_VIEW_CATEGORY;

@PropertyDetermineResourceStrategy(Page.ContainingPageResourceStrategy.class)
public class Page extends ContentDriven<PageContent> implements Comparable<Page> {

    public static final String DISPLAY_MODE_CSS_CLASS = PAGES_PREFIX + "display-mode";

    // static resource type determination

    /**
     * check the 'cpp:Page' type for a resource
     */
    public static boolean isPage(Resource resource) {
        return ResourceUtil.isResourceType(resource, NODE_TYPE_PAGE);
    }

    /**
     * check the 'cpp:PageContent' type for a resource
     */
    public static boolean isPageContent(Resource resource) {
        return ResourceUtil.isResourceType(resource, NODE_TYPE_PAGE_CONTENT);
    }

    // page attributes

    private transient Site site;
    private transient Page parent;

    public Page() {
    }

    protected Page(BeanContext context, Resource resource) {
        initialize(context, resource);
    }

    public Page(PageManager manager, BeanContext context, Resource resource) {
        this.pageManager = manager;
        initialize(context, resource);
    }

    @Override
    public int compareTo(@Nonnull Page page) {
        return getName().compareTo(page.getName());
    }

    // initializer extensions

    /** Compatible to Page{@link #determineResource(Resource)}. */
    public static class ContainingPageResourceStrategy implements DetermineResourceStategy {
        @Override
        public Resource determineResource(BeanContext beanContext, Resource requestResource) {
            return beanContext.getService(PageManager.class).getContainingPageResource(requestResource);
        }
    }

    @Override
    protected Resource determineResource(Resource initialResource) {
        return getPageManager().getContainingPageResource(initialResource);
    }

    @Override
    protected PageContent createContentModel(BeanContext context, Resource contentResource) {
        return new PageContent(context, contentResource);
    }

    // page hierarchy

    public Site getSite() {
        if (site == null) {
            site = getSiteManager().getContainingSite(this);
        }
        return site;
    }

    public String getSiteRelativePath() {
        final Site containingSite = getSite();
        final String sitePath = containingSite.getPath();
        final String path = getPath();
        return path.replace(sitePath, ".");
    }

    public boolean isTemplate() {
        return getResourceManager().isTemplate(this.getResource());
    }

    /**
     * returns 'true' it this page is the homepage ('isHomepage' would overlap with 'getHomepage' in a template)
     */
    public boolean isHome() {
        return getHomepage().getPath().equals(getPath());
    }

    /**
     * returns the homepage of the site which contains this page
     */
    public Homepage getHomepage() {
        Site site = getSite();
        return site != null ? site.getHomepage() : new Homepage(context, resource);
    }

    /**
     * returns the parent page if this page is not the homepage, otherwise this page itself
     */
    public Page getParentPage() {
        if (parent == null) {
            Resource parentRes = isHome() ? resource : resource.getParent();
            while (parent == null && parentRes != null) {
                if (isPage(parentRes)) {
                    parent = new Page(context, parentRes);
                } else {
                    parentRes = parentRes.getParent();
                }
            }
        }
        return parent;
    }

    /**
     * returns a list of all subpages which are accepted by the filter
     */
    public List<Page> getChildPages(ResourceFilter filter) {
        List<Page> children = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            if (Page.isPage(child) && filter.accept(child)) {
                Page page = new Page(context, child);
                children.add(page);
            }
        }
        return children;
    }

    // date / time properties

    public String getLastModifiedString() {
        return getContent().getLastModifiedString();
    }

    // rendering

    public String getHtmlLangAttribute() {
        Language language = getLanguage();
        if (language != null) {
            return "lang=\"" + language.getKey() + "\"";
        }
        return "";
    }

    public String getHtmlDirAttribute() {
        Language language = getLanguage();
        if (language != null) {
            String dir = language.getDirection();
            if (StringUtils.isNotBlank(dir)) {
                return "dir=\"" + dir + "\"";
            }
        }
        return "";
    }

    public String getHtmlClasses() {
        return StringUtils.join(collectHtmlClasses(new ArrayList<String>()), " ");
    }

    protected List<String> collectHtmlClasses(List<String> classes) {
        SlingHttpServletRequest request = context.getRequest();
        AccessMode accessMode = AccessMode.requestMode(request);
        if (AccessMode.AUTHOR == accessMode) {
            DisplayMode.Value displayMode = DisplayMode.requested(context);
            if (displayMode != null) {
                if (displayMode == DisplayMode.Value.DEVELOP) {
                    displayMode = DisplayMode.Value.EDIT;
                }
                classes.add(DISPLAY_MODE_CSS_CLASS + "_" + displayMode.name());
            }
        }
        String languageKey = getLanguageKey();
        if (StringUtils.isNotBlank(languageKey)) {
            classes.add(LANGUAGE_KEY + "_" + languageKey);
        }
        return classes;
    }

    public String getViewClientlibCategory() {
        return getInherited(PROP_VIEW_CATEGORY, DEFAULT_VIEW_CATEGORY);
    }

    public String getEditClientlibCategory() {
        return getInherited(PROP_EDIT_CATEGORY, DEFAULT_EDIT_CATEGORY);
    }

    // forward / redirect

    public String getSlingTarget() {
        return getProperty(PROP_SLING_TARGET, "");
    }

    public String getSlingTargetUrl() {
        String targetUrl = getSlingTarget();
        if (StringUtils.isNotBlank(targetUrl)) {
            SlingHttpServletRequest request = context.getRequest();
            targetUrl = LinkUtil.getUrl(request, targetUrl);
        }
        return targetUrl;
    }

    public boolean forward() throws ServletException, IOException {
        String target = getSlingTarget();
        if (StringUtils.isNotBlank(target)) {
            SlingHttpServletRequest request = context.getRequest();
            RequestDispatcherOptions options = new RequestDispatcherOptions();
            RequestDispatcher dispatcher = request.getRequestDispatcher(target, options);
            dispatcher.forward(request, context.getResponse());
            return true;
        }
        return false;
    }

    public boolean redirect() throws IOException {
        String targetUrl = getSlingTargetUrl();
        if (StringUtils.isNotBlank(targetUrl)) {
            SlingHttpServletResponse response = context.getResponse();
            response.sendRedirect(targetUrl);
            return true;
        }
        return false;
    }
}
