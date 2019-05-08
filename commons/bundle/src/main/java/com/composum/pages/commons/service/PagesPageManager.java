package com.composum.pages.commons.service;

import com.composum.pages.commons.PagesConfiguration;
import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.PagesConstants.ReferenceType;
import com.composum.pages.commons.filter.TemplateFilter;
import com.composum.pages.commons.model.ContentTypeFilter;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.PageContent;
import com.composum.pages.commons.replication.ReplicationManager;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.platform.staging.versions.PlatformVersionsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.pages.commons.PagesConstants.NODE_TYPE_PAGE;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Page Manager"
        }
)
public class PagesPageManager extends PagesContentManager<Page> implements PageManager {

    public static final String REF_PATH_ROOT = "/content/";
    public static final Pattern REF_VALUE_PATTERN = Pattern.compile("^" + REF_PATH_ROOT + ".+$");
    public static final Pattern REF_LINK_PATTERN = Pattern.compile("(href|src)=\"(" + REF_PATH_ROOT + "[^\"]+)?\"");

    public static final Map<String, Object> PAGE_PROPERTIES;
    public static final Map<String, Object> PAGE_CONTENT_PROPERTIES;

    static {
        PAGE_PROPERTIES = new HashMap<>();
        PAGE_PROPERTIES.put(JcrConstants.JCR_PRIMARYTYPE, NODE_TYPE_PAGE);
        PAGE_CONTENT_PROPERTIES = new HashMap<>();
        PAGE_CONTENT_PROPERTIES.put(JcrConstants.JCR_PRIMARYTYPE, PagesConstants.NODE_TYPE_PAGE_CONTENT);
    }

    @Reference
    protected PagesConfiguration pagesConfig;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected volatile PagesTenantSupport tenantSupport;

    @Reference
    protected ResourceManager resourceManager;

    @Reference
    protected ReplicationManager replicationManager;

    @Reference
    protected PlatformVersionsService versionsService;

    @Override
    public Page createBean(BeanContext context, Resource resource) {
        return new Page(this, context, resource);
    }

    @Override
    public Page getContainingPage(Model element) {
        return getContainingPage(element.getContext(), element.getResource());
    }

    @Override
    public Page getContainingPage(BeanContext context, Resource resource) {
        Page page = null;
        Resource pageResource = resourceManager.findContainingPageResource(resource);
        if (pageResource != null) {
            page = createBean(context, pageResource);
        }
        return page;
    }

    @Override
    public Resource getContainingPageResource(Resource resource) {
        Resource pageResource = resourceManager.findContainingPageResource(resource);
        // fallback to resource itself if no 'page' found
        return pageResource != null ? pageResource : resource;
    }

    @Override
    public Collection<Page> getPageTemplates(@Nonnull BeanContext context, @Nonnull Resource parent) {
        ResourceResolver resolver = context.getResolver();
        PageTemplateList result = new PageTemplateList(resolver);
        String tenantId = tenantSupport != null ? tenantSupport.getTenantId(parent) : null;
        for (String root : resolver.getSearchPath()) {
            String searchRootPath = root;
            if ("/apps/".equals(root) && StringUtils.isNotBlank(tenantId)) {
                searchRootPath = tenantSupport.getApplicationRoot(context, tenantId);
            }
            Resource searchRoot;
            if (StringUtils.isNotBlank(searchRootPath) && (searchRoot = resolver.getResource(searchRootPath)) != null) {
                Collection<Page> templates = getModels(context, NODE_TYPE_PAGE, searchRoot, TemplateFilter.INSTANCE);
                result.addAll(templates);
            }
        }
        ContentTypeFilter filter = new ContentTypeFilter(resourceManager, parent);
        String candidatePath = parent.getPath();
        for (int i = result.size(); --i >= 0; ) {
            Page templatePage = result.get(i);
            ResourceManager.Template template = resourceManager.toTemplate(templatePage.getResource());
            if (!filter.isAllowedChild(template,
                    resourceManager.getReference(resolver, candidatePath, template.getResourceType()))) {
                result.remove(i);
            }
        }
        result.sort();
        return result;
    }

    @Override
    @Nonnull
    public Page createPage(@Nonnull BeanContext context, @Nonnull Resource parent, @Nonnull String pageType,
                           @Nonnull String pageName, @Nullable String pageTitle, @Nullable String description,
                           boolean commit)
            throws RepositoryException, PersistenceException {

        Resource pageResource;
        ResourceResolver resolver = context.getResolver();
        checkExistence(resolver, parent, pageName);

        pageResource = resolver.create(parent, pageName, PAGE_PROPERTIES);
        Map<String, Object> contentProperties = new HashMap<>(PAGE_CONTENT_PROPERTIES);
        contentProperties.put(ResourceUtil.PROP_RESOURCE_TYPE, pageType);
        if (StringUtils.isNotBlank(pageTitle)) {
            contentProperties.put(ResourceUtil.PROP_TITLE, pageTitle);
        }
        if (StringUtils.isNotBlank(description)) {
            contentProperties.put(ResourceUtil.PROP_DESCRIPTION, description);
        }
        resolver.create(pageResource, JcrConstants.JCR_CONTENT, contentProperties);

        if (commit) {
            resolver.commit();
        }

        return instanceCreated(context, pageResource);
    }

    @Override
    @Nonnull
    @SuppressWarnings("Duplicates")
    public Page createPage(@Nonnull BeanContext context, @Nonnull Resource parent, @Nonnull Resource pageTemplate,
                           @Nonnull String pageName, @Nullable String pageTitle, @Nullable String description,
                           boolean commit)
            throws RepositoryException, PersistenceException {

        Resource pageResource;
        final ResourceResolver resolver = context.getResolver();
        checkExistence(resolver, parent, pageName);
        final SiteManager siteManager = context.getService(SiteManager.class);

        pageResource = resourceManager.createFromTemplate(new ResourceManager.TemplateContext() {

            @Override
            public ResourceResolver getResolver() {
                return resolver;
            }

            @Override
            public String applyTemplatePlaceholders(@Nonnull final Resource target, @Nonnull final String value) {
                Resource siteResource = siteManager.getContainingSiteResource(target);
                Resource pageResource = getContainingPageResource(target);
                String result = value.replaceAll("\\$\\{path}", target.getPath());
                result = result.replaceAll("\\$\\{page}", pageResource.getPath());
                if (siteResource != null) {
                    result = result.replaceAll("\\$\\{site}", siteResource.getPath());
                }
                if (!value.equals(result)) {
                    result = result.replaceAll("/[^/]+/\\.\\./", "/");
                }
                return result;
            }

        }, parent, pageName, pageTemplate, true);

        Resource content = Objects.requireNonNull(pageResource.getChild(JcrConstants.JCR_CONTENT));
        ModifiableValueMap values = Objects.requireNonNull(content.adaptTo(ModifiableValueMap.class));

        if (StringUtils.isNotBlank(pageTitle)) {
            values.put(ResourceUtil.PROP_TITLE, pageTitle);
        }
        if (StringUtils.isNotBlank(description)) {
            values.put(ResourceUtil.PROP_DESCRIPTION, description);
        }

        if (commit) {
            resolver.commit();
        }
        return instanceCreated(context, pageResource);
    }

    @Override
    public boolean deletePage(@Nonnull BeanContext context, @Nonnull String pagePath, boolean commit)
            throws PersistenceException {
        return deletePage(context, context.getResolver().getResource(pagePath), commit);
    }

    @Override
    public boolean deletePage(@Nonnull BeanContext context, @Nullable Resource pageResource, boolean commit)
            throws PersistenceException {

        if (pageResource != null) {
            ResourceResolver resolver = context.getResolver();
            if (LOG.isInfoEnabled()) {
                LOG.info("deletePage({})", pageResource.getPath());
            }

            resolver.delete(pageResource);

            if (commit) {
                resolver.commit();
            }
            return true;
        }
        return false;
    }

    @Override
    public void touch(@Nonnull BeanContext context, @Nonnull Resource resource, @Nullable Calendar time, boolean commit) {
        Page page = getContainingPage(context, resource);
        if (page != null) {
            touch(context, page, time, commit);
        }
    }

    @Override
    public void touch(@Nonnull BeanContext context, @Nonnull Page page, @Nullable Calendar time, boolean commit) {
        PageContent content = page.getContent();
        if (content != null) {
            if (time == null) {
                time = new GregorianCalendar();
                time.setTimeInMillis(System.currentTimeMillis());
            }
            Resource contentResource = content.getResource();
            ModifiableValueMap values = Objects.requireNonNull(contentResource.adaptTo(ModifiableValueMap.class));
            values.put(PagesConstants.PROP_LAST_MODIFIED, time);
            Session session = context.getResolver().adaptTo(Session.class);
            if (session != null) {
                String userId = session.getUserID();
                if (StringUtils.isNotBlank(userId)) {
                    values.put(PagesConstants.PROP_LAST_MODIFIED_BY, userId);
                }
            }
        }
    }

    /**
     * retrieve the collection of referrers of a content page
     *
     * @param page       the page
     * @param searchRoot the root in the repository for searching referrers
     * @param resolved   if 'true' only active references (in the same release) are determined
     * @return the collection of found resources
     */
    @Override
    @Nonnull
    public Collection<Resource> getReferrers(@Nonnull final Page page, @Nonnull final Resource searchRoot, boolean resolved) {
        Map<String, Resource> referrers = new TreeMap<>();
        ResourceFilter resolvedInReleaseFilter = versionsService.releaseAsResourceFilter(searchRoot, null, replicationManager);
        StringFilter propertyFilter = StringFilter.ALL;
        List<Resource> referringResources = new ArrayList<>();
        resourceManager.changeReferences(resolvedInReleaseFilter, propertyFilter, searchRoot, referringResources,
                true, page.getPath(), "");
        for (Resource resource : referringResources) {
            Resource referreringPage = getContainingPageResource(resource);
            if (referreringPage != null) {
                referrers.putIfAbsent(referreringPage.getPath(), referreringPage);
            }
        }
        return referrers.values();
    }

    /**
     * retrieve the collection of target resources of the content elements referenced by the page
     *
     * @param page       the page
     * @param type       the type of references; if 'null' all types are retrieved
     * @param unresolved if 'true' only unresolved references (not in the same release) are determined
     * @return the collection of found resources
     */
    @Override
    @Nonnull
    public Collection<Resource> getReferences(@Nonnull final Page page, @Nullable final ReferenceType type,
                                              boolean unresolved) {
        Map<String, Resource> references = new TreeMap<>();
        ResourceFilter resolvedInReleaseFilter = versionsService.releaseAsResourceFilter(page.getResource(), null, replicationManager);
        ResourceFilter unresolvedFilter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.none, resolvedInReleaseFilter);
        ResourceFilter resourceFilter = type != null
                ? new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                pagesConfig.getReferenceFilter(type), unresolvedFilter)
                : unresolvedFilter;
        StringFilter propertyFilter = StringFilter.ALL;
        Resource content = page.getContent().getResource();
        retrieveReferences(references, resourceFilter, propertyFilter, content);
        return references.values();
    }

    protected void retrieveReferences(@Nonnull Map<String, Resource> references,
                                      @Nonnull final ResourceFilter resourceFilter,
                                      @Nonnull final StringFilter propertyFilter,
                                      @Nonnull final Resource resource) {

        ResourceResolver resolver = resource.getResourceResolver();
        ValueMap values = resource.getValueMap();
        for (Map.Entry<String, Object> entry : values.entrySet()) {

            String key = entry.getKey();
            // check property by name
            if (propertyFilter.accept(key)) {

                Object value = entry.getValue();
                if (value instanceof String) {
                    retrieveReferences(references, resolver, resourceFilter, (String) value);

                } else if (value instanceof String[]) {
                    for (String val : (String[]) value) {
                        retrieveReferences(references, resolver, resourceFilter, val);
                    }
                }
            }
        }
        // recursive traversal
        for (Resource child : resource.getChildren()) {
            retrieveReferences(references, resourceFilter, propertyFilter, child);
        }
    }


    /**
     * adds all references found in the value to the set
     */
    protected void retrieveReferences(@Nonnull final Map<String, Resource> references,
                                      @Nonnull final ResourceResolver resolver,
                                      @Nonnull final ResourceFilter filter,
                                      @Nonnull final String value) {
        Resource resource;
        if (REF_VALUE_PATTERN.matcher(value).matches()) {
            // simple path value...
            if ((resource = resolver.getResource(value)) != null && filter.accept(resource)) {
                references.putIfAbsent(value, resource);
            }
        } else {
            // check for HTML patterns and extract all references if found
            Matcher matcher = REF_LINK_PATTERN.matcher(value);
            while (matcher.find()) {
                String path = matcher.group(2);
                if ((resource = resolver.getResource(path)) != null && filter.accept(resource)) {
                    references.putIfAbsent(path, resource);
                }
            }
        }
    }
}
