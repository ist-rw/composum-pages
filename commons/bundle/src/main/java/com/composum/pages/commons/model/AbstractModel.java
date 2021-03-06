/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.pages.commons.model;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.properties.Language;
import com.composum.pages.commons.model.properties.Languages;
import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.service.PageManager;
import com.composum.pages.commons.service.ResourceManager;
import com.composum.pages.commons.service.SiteManager;
import com.composum.pages.commons.service.VersionsService;
import com.composum.pages.commons.util.LinkUtil;
import com.composum.pages.commons.util.PagesUtil;
import com.composum.pages.commons.util.RequestUtil;
import com.composum.pages.commons.util.TagCssClasses;
import com.composum.platform.models.annotations.PropertyDefaults;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.InheritedValues;
import com.composum.sling.core.SlingBean;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.request.DomIdentifiers;
import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.platform.commons.request.AccessMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.composum.pages.commons.PagesConstants.PN_CATEGORY;
import static com.composum.pages.commons.PagesConstants.PN_DESCRIPTION;
import static com.composum.pages.commons.PagesConstants.PN_TITLE_KEYS;
import static com.composum.pages.commons.PagesConstants.RA_CURRENT_PAGE;
import static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE;
import static com.composum.pages.commons.servlet.PagesContentServlet.EDIT_RESOURCE_TYPE_KEY;
import static com.composum.platform.models.annotations.InternationalizationStrategy.I18NFOLDER;
import static com.composum.platform.commons.request.AccessMode.RA_ACCESS_MODE;

/**
 * the base class for all models beans in the pages context
 */
@PropertyDefaults(i18nStrategy = PagesInternationalizationStrategy.class,
        inheritanceType = InheritedValues.Type.contentRelated)
public abstract class AbstractModel implements SlingBean, Model {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractModel.class);

    /**
     * the property name for placeholder values (content hints)
     */
    public static final String PROP_PLACEHOLDER = "placeholder";

    /**
     * resource type to CSS: don't use basic types
     */
    public static final ResourceFilter CSS_BASE_TYPE_RESTRICTION =
            new ResourceFilter.ResourceTypeFilter(new StringFilter.BlackList("^(nt|sling):.*$"));

    /**
     * the list paths to use as I18N access path if I18N should be ignored
     */
    public static final List<String> IGNORE_I18N;

    static {
        IGNORE_I18N = new ArrayList<>();
        IGNORE_I18N.add(".");
    }

    // OSGi services

    /**
     * protected to allow set instance by the Page and Site model
     */
    protected transient SiteManager siteManager;
    protected transient PageManager pageManager;

    /**
     * lazy referenced services
     */
    private transient ResourceManager resourceManager;
    private transient VersionsService versionsService;

    // instance attributes

    /**
     * the instance of the scripting context for the model (initialized)
     */
    @Inject
    @Self
    protected BeanContext context;

    /**
     * the resource an related properties represented by this model (initialized)
     */
    protected Resource resource;
    protected ResourceResolver resolver;
    protected ValueMap properties;
    /**
     * inherited properties are initialized lazy
     */
    private transient InheritedValues inheritedValues;

    private transient Map<String, Object> propertiesMap;
    private transient Map<String, Object> inheritedMap;

    /**
     * current access mode (author/public) od the contexts request
     */
    private transient AccessMode accessMode;

    private transient DisplayMode.Value displayMode;

    private transient Page currentPage;
    private transient Page containingPage;
    private Boolean fromCurrentPage;

    private transient Locale locale;
    private transient Languages languages;
    private transient List<String> i18nPaths;

    private transient String cssBase;
    protected transient String domId;

    private transient String path;
    private transient String name;
    protected transient String type;

    protected transient String url;
    protected transient String title;
    private transient List<String> category;
    private transient String keywords;
    private transient String description;

    protected transient PagesConstants.ComponentType componentType;
    private transient Component component;

    private transient List<Resource> referrers;

    // Initializing

    public void initialize(BeanContext context, String path, String resourceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialize (" + context + ", " + path + ", " + resourceType + ")");
        }
        this.resolver = context.getResolver();
        Resource resource = resolver.resolve(path);
        if (ResourceUtil.isNonExistingResource(resource)) {
            this.type = resourceType;
        }
        initialize(context, resource);
    }

    /**
     * This basic initialization sets up the context and resource attributes only,
     * all the other attributes are set 'lazy' during their getter calls.
     *
     * @param context  the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     * @param resource the resource to use (normally the resource addressed by the request)
     */
    @Override
    public void initialize(BeanContext context, final Resource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialize (" + context + ", " + resource + ")");
        }
        this.context = context;
        if (this.resolver == null) {
            this.resolver = resource != null ? resource.getResourceResolver() : context.getResolver();
        }
        this.resource = determineResource(resource);
        if (this.resource == null) {
            this.resource = resource;
        } else {
            this.resolver = this.resource.getResourceResolver();
        }
        initializeWithResource(Objects.requireNonNull(this.resource));
    }

    /**
     * Uses the contexts 'resource' attribute for initialization (context.getResource()).
     *
     * @param context the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     */
    @Override
    public void initialize(BeanContext context) {
        Resource resource = context.getResource();
        initialize(context, resource);
    }

    /**
     * Initialization called during construction by Sling Models. Do not call otherwise.
     */
    @PostConstruct
    protected void initialize() {
        Validate.notNull(context);
        initialize(context);
    }

    /**
     * the extension hook to derive the models resource from the initializers resource
     */
    @Nullable
    protected Resource determineResource(@Nullable final Resource initialResource) {
        return initialResource;
    }

    /**
     * initialize all other properties after the final resource determination
     * extension hook for property initialization
     */
    protected void initializeWithResource(@Nonnull final Resource resource) {
        properties = resource.adaptTo(ValueMap.class);
        if (properties == null) {
            properties = new ValueMapDecorator(new HashMap<>());
        }
    }

    public Model getPropertyModel() {
        return this;
    }

    // component rendering

    public String getDomId() {
        if (domId == null) {
            domId = DomIdentifiers.getInstance(context).getElementId(this);
        }
        return domId;
    }

    public boolean isFromCurrentPage() {
        if (fromCurrentPage == null) {
            Page containingPage = getContainingPage();
            fromCurrentPage = containingPage != null && containingPage.equals(getCurrentPage());
        }
        return fromCurrentPage;
    }

    public boolean isPublicMode() {
        return AccessMode.PUBLIC == getAccessMode();
    }

    public boolean isPreviewMode() {
        return RequestUtil.isPreviewMode(context);
    }

    public boolean isAuthorMode() {
        return AccessMode.AUTHOR == getAccessMode();
    }

    public AccessMode getAccessMode() {
        if (accessMode == null) {
            accessMode = RequestUtil.getAccessMode(context.getRequest());
        }
        return accessMode;
    }

    /**
     * @return 'true' if the current display mode (the topmost of the stack) is an edit mode
     */
    public boolean isEditMode() {
        DisplayMode.Value mode = getDisplayMode();
        return mode == DisplayMode.Value.EDIT || mode == DisplayMode.Value.DEVELOP;
    }

    /**
     * @return 'true' if the requests display mode (the first mode of the stack) is an edit mode
     */
    public boolean isEditModeRequested() {
        DisplayMode.Value mode = DisplayMode.requested(context);
        return mode == DisplayMode.Value.EDIT || mode == DisplayMode.Value.DEVELOP;
    }

    /**
     * @return the current display mode (the topmost of the stack)
     */
    public DisplayMode.Value getDisplayMode() {
        if (displayMode == null) {
            displayMode = DisplayMode.current(context);
        }
        return displayMode;
    }

    /**
     * This is used by the 'component' tag to determine the CSS class base name for the component.
     */
    @Override
    @Nonnull
    public String getCssBase() {
        if (cssBase == null) {
            cssBase = buildCssBase();
        }
        return cssBase;
    }

    /**
     * Creates the CSS base name for the components rendering (extension hook).
     */
    protected String buildCssBase() {
        String type = getCssBaseType();
        return StringUtils.isNotBlank(type) ? TagCssClasses.cssOfType(type) : null;
    }

    /**
     * Returns the resource type which should be used to build the CSS base (extension hook).
     */
    protected String getCssBaseType() {
        return CSS_BASE_TYPE_RESTRICTION.accept(resource) ? resource.getResourceType() : null;
    }

    //

    /**
     * public access to the context
     */
    @Override
    public BeanContext getContext() {
        return context;
    }

    /**
     * public access to the resource
     */
    @Override
    @Nonnull
    public Resource getResource() {
        return resource;
    }

    @Override
    @Nonnull
    public String getPath() {
        if (path == null) {
            path = resource.getPath();
        }
        return path;
    }

    @Override
    @Nonnull
    public String getName() {
        if (name == null) {
            name = resource.getName();
        }
        return name;
    }

    @Override
    @Nonnull
    public String getType() {
        if (type == null) {
            type = resource.getResourceType();
        }
        return type;
    }

    public String getEncodedReference() {
        String typeHint = null;
        SlingHttpServletRequest request = getContext().getRequest();
        if (request != null) {
            typeHint = (String) request.getAttribute(EDIT_RESOURCE_TYPE_KEY);
        }
        return PagesUtil.getEncodedReference(getResource(), typeHint);
    }

    @Override
    public String getPathHint() {
        return getPathHint(getPath());
    }

    @Override
    public String getTypeHint() {
        return getTypeHint(getType());
    }

    public static String getPathHint(Resource resource) {
        return getPathHint(resource.getPath());
    }

    public static String getPathHint(String path) {
        path = path.replaceAll("^/(sites|content|apps|libs)/.*/jcr:content$", "");
        path = path.replaceAll("^/(sites|content|apps|libs)/.*/jcr:content/", "./");
        path = path.replaceAll("/[^/]*$", "/");
        return path;
    }

    public static String getTypeHint(Resource resource) {
        return getTypeHint(resource.getResourceType());
    }

    public static String getTypeHint(String type) {
        if (type != null) {
            type = type.replaceAll("/edit/(dialog|tile|tree|toolbar|context)(/.+)?$", "");
            type = type.replaceAll("^/(sites|apps|libs)/", "");
            type = type.replaceAll("^(.*/)?composum/(.*/)?pages/", "$2");
            type = type.replaceAll("/components?/", "/");
            type = type.replaceAll("/containers?/", "/");
            type = type.replaceAll("/composed/", "/");
            type = type.replaceAll("/elements?/", "/");
        }
        return type;
    }

    public List<Resource> getReferrers() {
        if (referrers == null) {
            referrers = new ArrayList<>();
            try {
                getResourceManager().changeReferences(ResourceFilter.ALL, StringFilter.ALL,
                        Objects.requireNonNull(getContext().getResolver().getResource("/content")),
                        referrers, true, getPath(), "");
            } catch (PersistenceException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return referrers;
    }

    // component

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new Component(context, getResource());
        }
        return component;
    }

    @Override
    public PagesConstants.ComponentType getComponentType() {
        if (componentType == null) {
            componentType = PagesConstants.ComponentType.typeOf(resolver, resource, null);
        }
        return componentType;
    }

    //

    @Override
    @Nonnull
    public String getTitle() {
        if (title == null) {
            title = getProperty(getLocale(), "", getTitleKeys());
        }
        return title;
    }

    protected String[] getTitleKeys() {
        return PN_TITLE_KEYS;
    }

    /**
     * @return the title for the tile rendering, maybe a part of a value if not title property set
     */
    @Nonnull
    @Override
    public String getTileTitle() {
        return getTitle();
    }

    @Nonnull
    public List<String> getCategory() {
        if (category == null) {
            category = Arrays.asList(getInherited(PN_CATEGORY, new String[0]));
        }
        return category;
    }

    @Nonnull
    public String getKeywords() {
        if (keywords == null) {
            SlingHttpServletRequest request = context.getRequest();
            StringBuilder builder = new StringBuilder();
            for (String keyword : getCategory()) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(I18N.get(request, keyword));
            }
            keywords = builder.toString();
        }
        return keywords;
    }

    @Override
    @Nonnull
    public String getDescription() {
        if (description == null) {
            description = getProperty(getLocale(), "", PN_DESCRIPTION, ResourceUtil.PROP_DESCRIPTION);
        }
        return description;
    }

    /**
     * Returns the URL to the resource of this bean (mapped and with the appropriate extension).
     *
     * @see LinkUtil#getUrl(SlingHttpServletRequest, String)
     */
    @Override
    @Nonnull
    public String getUrl() {
        if (url == null) {
            SlingHttpServletRequest request = context.getRequest();
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            url = LinkUtil.getUrl(request, getPath(), pathInfo.getSelectorString(), null);
        }
        return url;
    }

    // i18n - language support

    @Nonnull
    public Locale getLocale() {
        if (locale == null) {
            locale = getContext().getAttribute(RA_STICKY_LOCALE, Locale.class);
            if (locale == null) {
                Language language = getLanguage();
                locale = language.getLocale();
            }
        }
        return locale;
    }

    @Nonnull
    public String getLanguageKey() {
        Language language = getLanguage();
        return language.getKey();
    }

    @Override
    @Nonnull
    public Language getLanguage() {
        return getLanguages().getLanguage();
    }

    @Override
    @Nonnull
    public Languages getLanguages() {
        if (languages == null) {
            languages = Languages.get(context);
            if (languages == null) {
                Languages.set(context, getResource());
                languages = Languages.get(context);
            }
        }
        return languages;
    }

    @Nonnull
    protected List<String> getI18nPaths() {
        if (i18nPaths == null) {
            i18nPaths = I18NFOLDER.getI18nPaths(getLocale());
        }
        return i18nPaths;
    }

    // resource properties

    @Override
    @Nonnull
    public <T> T getProperty(@Nonnull final String key, @Nonnull final T defaultValue) {
        return getProperty(key, getLocale(), defaultValue);
    }

    @Override
    @Nullable
    public <T> T getProperty(@Nonnull final String key, @Nonnull final Class<T> type) {
        return getProperty(key, getLocale(), type);
    }

    @Nonnull
    public <T> T getProperty(@Nonnull final String key, @Nullable final Locale locale, @Nonnull final T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = getProperty(key, locale, type);
        return value != null ? value : defaultValue;
    }

    @Nullable
    public <T> T getProperty(@Nonnull final String key, @Nullable final Locale locale,
                             @Nonnull final Class<T> type) {
        return getProperty(key, type, locale != null ? getI18nPaths() : IGNORE_I18N);
    }

    @Override
    @Nonnull
    public <T> T getProperty(@Nullable final Locale locale, @Nonnull final T defaultValue, String... keys) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value;
        for (String key : keys) {
            if ((value = getProperty(key, locale, type)) != null) {
                return value;
            }
        }
        return defaultValue;
    }

    @Nullable
    protected <T> T getProperty(@Nonnull final String key, @Nonnull final Class<T> type,
                                @Nonnull final List<String> pathsToTry) {
        T value;
        if (properties != null) {
            for (String path : pathsToTry) {
                if ((value = properties.get(path + '/' + key, type)) != null) {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    @Nonnull
    public ValueMap getValueMap() {
        return properties;
    }

    /**
     * the generic map for direct use in templates
     */
    @Nonnull
    public Map<String, Object> getProperties() {
        if (propertiesMap == null) {
            propertiesMap = new GenericProperties();
        }
        return propertiesMap;
    }

    // inherited properties

    @Override
    @Nonnull
    public <T> T getInherited(@Nonnull final String key, @Nonnull final T defaultValue) {
        return getInherited(key, getLocale(), defaultValue);
    }

    @Override
    @Nullable
    public <T> T getInherited(@Nonnull final String key, @Nonnull final Class<T> type) {
        return getInherited(key, getLocale(), type);
    }

    @Nonnull
    protected <T> T getInherited(@Nonnull final String key, @Nullable final Locale locale,
                                 @Nonnull final T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = getInherited(key, locale, type);
        return value != null ? value : defaultValue;
    }

    @Nullable
    protected <T> T getInherited(@Nonnull final String key, @Nullable final Locale locale,
                                 @Nonnull final Class<T> type) {
        T value = getProperty(key, locale, type);
        if (value == null) {
            value = getInherited(key, type, locale != null ? getI18nPaths() : IGNORE_I18N);
        }
        return value;
    }

    @Override
    @Nonnull
    public <T> T getInherited(@Nullable Locale locale, @Nonnull T defaultValue, String... keys) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value;
        for (String key : keys) {
            if ((value = getInherited(key, locale, type)) != null) {
                return value;
            }
        }
        return defaultValue;
    }

    @Nullable
    protected <T> T getInherited(@Nonnull final String key, @Nonnull final Class<T> type,
                                 @Nonnull final List<String> pathsToTry) {
        T value;
        for (String path : pathsToTry) {
            InheritedValues values = getInheritedValues();
            if ((value = values.get(path + '/' + key, type)) != null) {
                return value;
            }
        }
        return null;
    }

    @Nonnull
    protected InheritedValues getInheritedValues() {
        if (inheritedValues == null) {
            inheritedValues = createInheritedValues(getResource());
        }
        return inheritedValues;
    }

    /**
     * the generic map for direct use in templates
     */
    @Nonnull
    public Map<String, Object> getInherited() {
        if (inheritedMap == null) {
            inheritedMap = new GenericInherited();
        }
        return inheritedMap;
    }

    /**
     * create the inherited properties strategy (extension hook, defaults to an instance of InheritedValues)
     */
    @Nonnull
    protected InheritedValues createInheritedValues(@Nonnull final Resource resource) {
        return new InheritedValues(resource);
    }

    // generic property access via generic Map for direct use in templates

    public abstract class GenericMap extends HashMap<String, Object> {

        public static final String UNDEFINED = "<undefined>";

        /**
         * delegates each 'get' to the localized methods and caches the result
         */
        @Override
        @Nullable
        public Object get(@Nonnull final Object key) {
            Object value = super.get(key);
            if (value == null) {
                value = getValue((String) key);
                super.put((String) key, value != null ? value : UNDEFINED);
            }
            return value != UNDEFINED ? value : null;
        }

        protected abstract Object getValue(String key);
    }

    public class GenericProperties extends GenericMap {

        @Override
        @Nullable
        public Object getValue(@Nonnull final String key) {
            return getProperty(key, Object.class);
        }

    }

    public class GenericInherited extends GenericMap {

        @Override
        @Nullable
        public Object getValue(@Nonnull final String key) {
            return getInherited(key, Object.class);
        }

    }

    // Sites & Pages

    /**
     * the requested page referenced by the current HTTP request
     */
    @Override
    @Nullable
    public Page getCurrentPage() {
        if (currentPage == null) {
            currentPage = context.getAttribute(RA_CURRENT_PAGE, Page.class);
        }
        return currentPage;
    }

    @Override
    @Nullable
    public Page getContainingPage() {
        if (containingPage == null) {
            containingPage = getPageManager().getContainingPage(this);
        }
        return containingPage;
    }

    @Nonnull
    public PageManager getPageManager() {
        if (pageManager == null) {
            pageManager = Objects.requireNonNull(context.getService(PageManager.class));
        }
        return pageManager;
    }

    @Nonnull
    public SiteManager getSiteManager() {
        if (siteManager == null) {
            siteManager = Objects.requireNonNull(context.getService(SiteManager.class));
        }
        return siteManager;
    }

    @Nonnull
    public ResourceManager getResourceManager() {
        if (resourceManager == null) {
            resourceManager = Objects.requireNonNull(context.getService(ResourceManager.class));
        }
        return resourceManager;
    }

    @Nonnull
    public VersionsService getVersionsService() {
        if (versionsService == null) {
            versionsService = Objects.requireNonNull(context.getService(VersionsService.class));
        }
        return versionsService;
    }

    // Object

    @Override
    public String toString() {
        String ref = super.toString();
        StringBuilder builder = new StringBuilder(ref.substring(ref.lastIndexOf(".") + 1));
        builder.append("{").append(getPath()).append("}");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return getHashKey().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Model && ((Model) other).getHashKey().equals(getHashKey());
    }

    @Nonnull
    @Override
    public String getHashKey() {
        return getClass().getName() + "#" + getPath();
    }
}
