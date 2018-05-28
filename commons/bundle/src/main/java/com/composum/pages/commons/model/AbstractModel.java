package com.composum.pages.commons.model;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.properties.Language;
import com.composum.pages.commons.model.properties.Languages;
import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.service.PageManager;
import com.composum.pages.commons.service.ResourceManager;
import com.composum.pages.commons.service.SiteManager;
import com.composum.pages.commons.service.VersionsService;
import com.composum.pages.commons.util.TagCssClasses;
import com.composum.pages.commons.util.ValueHashMap;
import com.composum.platform.models.annotations.PropertyDefaults;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.InheritedValues;
import com.composum.sling.core.SlingBean;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.request.DomIdentifiers;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.platform.security.AccessMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.composum.pages.commons.taglib.DefineObjectsTag.CURRENT_PAGE;
import static com.composum.platform.models.annotations.InternationalizationStrategy.I18NFOLDER;
import static com.composum.sling.platform.security.PlatformAccessFilter.ACCESS_MODE_KEY;

/**
 * the base class for all models beans in the pages context
 */
@PropertyDefaults(i18nStrategy = PagesInternationalizationStrategy.class,
        inheritanceType = InheritedValues.Type.contentRelated)
public abstract class AbstractModel implements SlingBean, Model {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractModel.class);

    /** general property names */
    public static final String PROP_TITLE = "title";
    public static final String[] PROP_TITLE_KEYS = new String[]{PROP_TITLE, ResourceUtil.PROP_TITLE};
    public static final String PROP_DESCRIPTION = "description";

    /** the property name for placeholder values (content hints) */
    public static final String PROP_PLACEHOLDER = "placeholder";

    /** resource type to CSS: don't use basic types */
    public static final ResourceFilter CSS_BASE_TYPE_RESTRICTION =
            new ResourceFilter.ResourceTypeFilter(new StringFilter.BlackList("^(nt|sling):.*$"));

    /** the list paths to use as I18N access path if I18N should be ignored */
    public static final List<String> IGNORE_I18N;

    static {
        IGNORE_I18N = new ArrayList<>();
        IGNORE_I18N.add(".");
    }

    /** the instance of the scripting context for the model (initialized) */
    @Inject
    @Self
    protected BeanContext context;
    protected transient SiteManager siteManager;
    protected transient PageManager pageManager;
    protected transient ResourceManager resourceManager;
    private transient VersionsService versionsService;

    /** the resource an related properties represented by this model (initialized) */
    protected Resource resource;
    protected ResourceResolver resolver;
    protected ValueMap properties;
    /** inherited properties are initialized lazy */
    private transient InheritedValues inheritedValues;

    private transient Map<String, Object> propertiesMap;
    private transient Map<String, Object> inheritedMap;

    /** current access mode (author/public) od the contexts request */
    private transient AccessMode accessMode;

    private transient Page currentPage;
    private transient Page containingPage;

    private transient Locale locale;
    private transient Languages languages;
    private transient List<String> i18nPaths;

    private transient String cssBase;
    protected transient String domId;

    private transient String path;
    private transient String name;
    protected transient String type;

    private transient String url;
    private transient String title;
    private transient String description;

    private transient PagesConstants.ComponentType componentType;
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
    public void initialize(BeanContext context, Resource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialize (" + context + ", " + resource + ")");
        }
        this.context = context;
        if (this.resolver == null && resource != null) {
            this.resolver = resource.getResourceResolver();
        }
        this.resource = determineResource(resource);
        if (this.resource != null) {
            if (this.resolver == null) {
                this.resolver = this.resource.getResourceResolver();
            }
            initializeWithResource(this.resource);
        }
    }

    /**
     * Uses the contexts 'resource' attribute for initialization (context.getResource()).
     *
     * @param context the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     */
    public void initialize(BeanContext context) {
        Resource resource = context.getResource();
        initialize(context, resource);
    }

    /** Initialization called during construction by Sling Models. Do not call otherwise. */
    @PostConstruct
    protected void initialize() {
        Validate.notNull(context);
        initialize(context);
    }

    /**
     * the extension hook to derive the models resource from the initializers resource
     */
    protected Resource determineResource(Resource initialResource) {
        return initialResource;
    }

    /**
     * initialize all other properties after the final resource determination
     * extension hook for property initialization
     */
    protected void initializeWithResource(Resource resource) {
        properties = resource.adaptTo(ValueMap.class);
        if (properties == null) {
            properties = new ValueHashMap();
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

    public boolean isPublicMode() {
        AccessMode accessMode = getAccessMode();
        return accessMode != null && accessMode == AccessMode.PUBLIC;
    }

    public boolean isAuthorMode() {
        AccessMode accessMode = getAccessMode();
        return accessMode != null && accessMode == AccessMode.AUTHOR;
    }

    public AccessMode getAccessMode() {
        if (accessMode == null) {
            String value = (String) context.getRequest().getAttribute(ACCESS_MODE_KEY);
            accessMode = StringUtils.isNotBlank(value)
                    ? AccessMode.valueOf(value)
                    : null;
        }
        return accessMode;
    }

    public boolean isEditMode() {
        DisplayMode.Value mode = getDisplayMode();
        return mode == DisplayMode.Value.EDIT || mode == DisplayMode.Value.DEVELOP;
    }

    public DisplayMode.Value getDisplayMode() {
        return DisplayMode.requested(context);
    }

    /**
     * This is used by the 'component' tag to determine the CSS class base name for the component.
     */
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

    /** public access to the context */
    public BeanContext getContext() {
        return context;
    }

    /** public access to the resource */
    public Resource getResource() {
        return resource;
    }

    public String getPath() {
        if (path == null) {
            path = resource.getPath();
        }
        return path;
    }

    public String getName() {
        if (name == null) {
            name = resource.getName();
        }
        return name;
    }

    public String getType() {
        if (type == null) {
            type = resource.getResourceType();
        }
        return type;
    }

    public String getPathHint() {
        return getPathHint(getPath());
    }

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
            type = type.replaceAll("^/(sites|apps|libs)/(.*)$", "$2");
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
            getResourceManager().changeReferences(ResourceFilter.ALL, StringFilter.ALL,
                    getContext().getResolver().getResource("/content"), referrers, true, getPath(), "");
        }
        return referrers;
    }

    // component

    public Component getComponent() {
        if (component == null) {
            component = new Component(context, getResource());
        }
        return component;
    }

    public PagesConstants.ComponentType getComponentType() {
        if (componentType == null) {
            componentType = PagesConstants.ComponentType.typeOf(resolver, resource, null);
        }
        return componentType;
    }

    //

    public String getTitle() {
        if (title == null) {
            title = getProperty(getLocale(), "", getTitleKeys());
        }
        return title;
    }

    protected String[] getTitleKeys() {
        return PROP_TITLE_KEYS;
    }

    public String getDescription() {
        if (description == null) {
            description = getProperty(getLocale(), "", ResourceUtil.PROP_DESCRIPTION, PROP_DESCRIPTION);
        }
        return description;
    }

    /**
     * Returns the URL to the resource of this bean (mapped and with the appropriate extension).
     *
     * @see LinkUtil#getUrl(SlingHttpServletRequest, String)
     */
    public String getUrl() {
        if (url == null) {
            SlingHttpServletRequest request = context.getRequest();
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            url = LinkUtil.getUrl(request, getPath(), pathInfo.getSelectorString(), null);
        }
        return url;
    }

    // i18n - language support

    public Locale getLocale() {
        if (locale == null) {
            locale = context.getRequest().adaptTo(Locale.class);
        }
        return locale;
    }

    public String getLanguageKey() {
        Language language = getLanguage();
        if (language != null) {
            return language.getKey();
        }
        Locale locale = getLocale();
        if (locale != null) {
            return locale.toString();
        }
        return "";
    }

    public Language getLanguage() {
        return getLanguages().getLanguage(getLocale());
    }

    public Languages getLanguages() {
        if (languages == null) {
            languages = Languages.get(context);
        }
        return languages;
    }

    protected List<String> getI18nPaths() {
        if (i18nPaths == null) {
            i18nPaths = I18NFOLDER.getI18nPaths(getLocale());
        }
        return i18nPaths;
    }

    // resource properties

    public <T> T getProperty(String key, T defaultValue) {
        return getProperty(key, getLocale(), defaultValue);
    }

    public <T> T getProperty(String key, Class<T> type) {
        return getProperty(key, getLocale(), type);
    }

    protected <T> T getProperty(String key, Locale locale, T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = getProperty(key, locale, type);
        return value != null ? value : defaultValue;
    }

    protected <T> T getProperty(String key, Locale locale, Class<T> type) {
        return getProperty(key, type, locale != null ? getI18nPaths() : IGNORE_I18N);
    }

    public <T> T getProperty(Locale locale, T defaultValue, String... keys) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value;
        for (String key : keys) {
            if ((value = getProperty(key, locale, type)) != null) {
                return value;
            }
        }
        return defaultValue;
    }

    protected <T> T getProperty(String key, Class<T> type, List<String> pathsToTry) {
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

    /**
     * the generic map for direct use in templates
     */
    public Map<String, Object> getProperties() {
        if (propertiesMap == null) {
            propertiesMap = new GenericProperties();
        }
        return propertiesMap;
    }

    // inherited properties

    public <T> T getInherited(String key, T defaultValue) {
        return getInherited(key, getLocale(), defaultValue);
    }

    public <T> T getInherited(String key, Class<T> type) {
        return getInherited(key, getLocale(), type);
    }

    protected <T> T getInherited(String key, Locale locale, T defaultValue) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value = getInherited(key, locale, type);
        return value != null ? value : defaultValue;
    }

    protected <T> T getInherited(String key, Locale locale, Class<T> type) {
        T value = getProperty(key, locale, type);
        if (value == null) {
            value = getInherited(key, type, locale != null ? getI18nPaths() : IGNORE_I18N);
        }
        return value;
    }

    public <T> T getInherited(Locale locale, T defaultValue, String... keys) {
        Class<T> type = PropertyUtil.getType(defaultValue);
        T value;
        for (String key : keys) {
            if ((value = getInherited(key, locale, type)) != null) {
                return value;
            }
        }
        return defaultValue;
    }

    protected <T> T getInherited(String key, Class<T> type, List<String> pathsToTry) {
        T value;
        for (String path : pathsToTry) {
            InheritedValues values = getInheritedValues();
            if (values != null) {
                if ((value = values.get(path + '/' + key, type)) != null) {
                    return value;
                }
            }
        }
        return null;
    }

    protected InheritedValues getInheritedValues() {
        if (inheritedValues == null) {
            if (resource != null) {
                inheritedValues = createInheritedValues(resource);
            }
        }
        return inheritedValues;
    }

    /**
     * the generic map for direct use in templates
     */
    public Map<String, Object> getInherited() {
        if (inheritedMap == null) {
            inheritedMap = new GenericInherited();
        }
        return inheritedMap;
    }

    /**
     * create the inherited properties strategy (extension hook, defaults to an instance of InheritedValues)
     */
    protected InheritedValues createInheritedValues(Resource resource) {
        return new InheritedValues(resource);
    }

    // generic property access via generic Map for direct use in templates

    public abstract class GenericMap extends HashMap<String, Object> {

        public static final String UNDEFINED = "<undefined>";

        /**
         * delegates each 'get' to the localized methods and caches the result
         */
        @Override
        public Object get(Object key) {
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
        public Object getValue(String key) {
            return getProperty(key, Object.class);
        }

    }

    public class GenericInherited extends GenericMap {

        @Override
        public Object getValue(String key) {
            return getInherited(key, Object.class);
        }

    }

    // Sites & Pages

    /**
     * the requested page referenced by the current HTTP request
     */
    public Page getCurrentPage() {
        if (currentPage == null) {
            currentPage = context.getAttribute(CURRENT_PAGE, Page.class);
        }
        return currentPage;
    }

    public Page getContainingPage() {
        if (containingPage == null) {
            containingPage = getPageManager().getContainingPage(this);
        }
        return containingPage;
    }

    public PageManager getPageManager() {
        if (pageManager == null) {
            pageManager = context.getService(PageManager.class);
        }
        return pageManager;
    }

    public SiteManager getSiteManager() {
        if (siteManager == null) {
            siteManager = context.getService(SiteManager.class);
        }
        return siteManager;
    }

    public ResourceManager getResourceManager() {
        if (resourceManager == null) {
            resourceManager = context.getService(ResourceManager.class);
        }
        return resourceManager;
    }

    public VersionsService getVersionsService() {
        if (versionsService == null) {
            versionsService = context.getService(VersionsService.class);
        }
        return versionsService;
    }
}
