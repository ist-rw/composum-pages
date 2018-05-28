package com.composum.pages.commons.model;

import com.composum.pages.commons.service.PageManager;
import com.composum.pages.commons.service.SiteManager;
import com.composum.platform.models.annotations.DetermineResourceStategy;
import com.composum.platform.models.annotations.PropertyDetermineResourceStrategy;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.composum.pages.commons.PagesConstants.DEFAULT_HOMEPAGE_PATH;
import static com.composum.pages.commons.PagesConstants.NODE_TYPE_SITE;
import static com.composum.pages.commons.PagesConstants.NODE_TYPE_SITE_CONFIGURATION;
import static com.composum.pages.commons.PagesConstants.PROP_HOMEPAGE;

@PropertyDetermineResourceStrategy(Site.ContainingSiteResourceStrategy.class)
public class Site extends ContentDriven<SiteConfiguration> implements Comparable<Site> {

    private static final Logger LOG = LoggerFactory.getLogger(Site.class);

    public static final String PUBLIC_MODE_IN_PLACE = "inPlace";
    public static final String PUBLIC_MODE_VERSIONS = "versions";
    public static final String PUBLIC_MODE_LIVE = "live";

    public static final String PROP_PUBLIC_MODE = "publicMode";
    public static final String DEFAULT_PUBLIC_MODE = PUBLIC_MODE_IN_PLACE;

    public static final String RELEASE_LABEL_PREFIX = "composum-release-";

    // static resource type determination

    /**
     * check the 'cpp:Site' type for a resource
     */
    public static boolean isSite(Resource resource) {
        return ResourceUtil.isResourceType(resource, NODE_TYPE_SITE);
    }

    /**
     * check the 'cpp:SiteConfiguration' type for a resource
     */
    public static boolean isSiteConfiguration(Resource resource) {
        return ResourceUtil.isResourceType(resource, NODE_TYPE_SITE_CONFIGURATION);
    }

    // site attributes

    private transient String publicMode;
    private transient Homepage homepage;

    private transient Collection<Page> modifiedPages;
    private transient Collection<Page> unreleasedPages;

    public Site() {
    }

    protected Site(BeanContext context, Resource resource) {
        initialize(context, resource);
    }

    public Site(SiteManager manager, BeanContext context, Resource resource) {
        this.siteManager = manager;
        initialize(context, resource);
    }

    @Override
    public int compareTo(@Nonnull Site site) {
        return getName().compareTo(site.getName());
    }

    // initializer extensions

    /** Compatible to {@link Site#determineResource(Resource)}. */
    public static class ContainingSiteResourceStrategy implements DetermineResourceStategy {
        @Override
        public Resource determineResource(BeanContext beanContext, Resource requestResource) {
            return beanContext.getService(SiteManager.class).getContainingSiteResource(requestResource);
        }
    }

    @Override
    protected Resource determineResource(Resource initialResource) {
        return getSiteManager().getContainingSiteResource(initialResource);
    }

    @Override
    protected SiteConfiguration createContentModel(BeanContext context, Resource contentResource) {
        return new SiteConfiguration(context, contentResource);
    }

    // Site properties

    public boolean isTemplate() {
        return getResourceManager().isTemplate(this.getResource());
    }

    @Override
    public String getTitle() {
        String title = super.getTitle();
        return StringUtils.isNotBlank(title) ? title : getName();
    }

    // site hierarchy

    public Homepage getHomepage() {
        if (homepage == null) {
            PageManager pageManager = getPageManager();
            String homepagePath = getProperty(PROP_HOMEPAGE, null, DEFAULT_HOMEPAGE_PATH);
            Resource homepageRes = resource.getChild(homepagePath);
            if (homepageRes != null) {
                homepage = new Homepage(pageManager, context, homepageRes);
            } else {
                homepage = new Homepage(pageManager, context, resource); // use itself as homepage
            }
        }
        return homepage;
    }

    // releases

    /**
     * returns the 'publicMode' property value of this site
     */
    public String getPublicMode() {
        if (publicMode == null) {
            publicMode = getProperty(PROP_PUBLIC_MODE, null, DEFAULT_PUBLIC_MODE);
        }
        return publicMode;
    }

    /**
     * retrieves the release label for a release category ('public', 'preview')
     * the content of this release is delivered if a public request in the category is performed
     */
    public String getReleaseLabel(String category) {
        Resource releases = content.resource.getChild("releases");
        if (releases != null) {
            category = category.toLowerCase();
            for (Resource release : releases.getChildren()) {
                ValueMap values = release.adaptTo(ValueMap.class);
                String key = values.get("key", "");
                if (StringUtils.isNotBlank(key)) {
                    if (key.equals(category)) {
                        return RELEASE_LABEL_PREFIX + key;
                    } else {
                        List<String> categories = Arrays.asList(values.get("categories", new String[0]));
                        if (categories.contains(category)) {
                            return RELEASE_LABEL_PREFIX + key;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * return the list of content releases of this site
     */
    public List<Release> getReleases() {
        List<Release> result = new ArrayList<>();
        Resource releases = content.resource.getChild("releases");
        if (releases != null) {
            for (Resource releaseResource : releases.getChildren()) {
                final Release release = new Release(context, releaseResource);
                result.add(release);
            }
        }
        return result;
    }

    public Collection<Page> getModifiedPages() {
        if (modifiedPages == null) {
            modifiedPages = getVersionsService().findModifiedPages(getContext(), getResource());
        }
        return modifiedPages;
    }

    public Collection<Page> getUnreleasedPages() {
        if (unreleasedPages == null) {
            final List<Release> releases = getReleases();
            final Release release = releases.isEmpty() ? null : releases.get(releases.size() - 1);
            unreleasedPages = getUnreleasedPages(release);
        }
        return unreleasedPages;
    }

    public Collection<Page> getUnreleasedPages(Release releaseToCheck) {
        Collection<Page> result;
        try {
            result = getVersionsService().findUnreleasedPages(getContext(), getResource(), releaseToCheck);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            result = new ArrayList<>();
        }
        return result;
    }
}
