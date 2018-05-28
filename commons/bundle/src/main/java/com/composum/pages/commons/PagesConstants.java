package com.composum.pages.commons;

import com.composum.pages.commons.model.Container;
import com.composum.pages.commons.model.Element;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.Site;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface PagesConstants {

    String ENCODING = "UTF-8";

    String CPP_NAMESPACE = "cpp";
    String CPP_PREFIX = CPP_NAMESPACE + ":";
    String COMPOSUM_PREFIX = "composum-";
    String PAGES_PREFIX = COMPOSUM_PREFIX + "pages-";

    String LANGUAGE_KEY = PAGES_PREFIX + "language";

    /** request aspects */

    String ACCESS_MODE_REQ_PARAM = "pages.access";
    String DISPLAY_MODE_REQ_PARAM = "pages.mode";
    String LOCALE_REQUEST_PARAM = "pages.locale";

    String ACCESS_MODE_ATTR = "composum.pages.request.access";
    String DISPLAY_MODE_ATTR = "composum.pages.request.display";
    String PAGES_LOCALE_ATTR = "composum.pages.request.locale";

    String FRAME_CONTEXT_ATTR = "composum-pages-frame";

    /** Component declarations */

    String NODE_TYPE_COMPONENT = CPP_PREFIX + "Component";
    String PROP_COMPONENT_TYPE = "componentType";

    /** Content elements */

    String NODE_TYPE_SOMETHING = "nt:unstructured";
    String NODE_TYPE_ELEMENT = CPP_PREFIX + "Element";
    String NODE_TYPE_CONTAINER = CPP_PREFIX + "Container";

    String PROP_ALLOWED_CONTAINERS = "allowedContainers";
    String PROP_ALLOWED_ELEMENTS = "allowedElements";

    /** Page */

    String NODE_TYPE_PAGE = CPP_PREFIX + "Page";
    String NODE_TYPE_PAGE_CONTENT = CPP_PREFIX + "PageContent";

    String META_NODE_NAME = CPP_PREFIX + "metaData";
    String META_NODE_TYPE = CPP_PREFIX + "MetaData";

    String PROP_ALLOWED_PARENTS = "allowedParents";
    String PROP_ALLOWED_CHILDREN = "allowedChildren";

    String NAVIGATION_PROPS = "navigation/";
    String PROP_HIDE_IN_NAV = NAVIGATION_PROPS + "hideInNav";
    String PROP_NAV_TITLE = NAVIGATION_PROPS + "title";

    String PROP_SLING_TARGET = "sling:target";

    String SUBNODE_STYLE = "style/";
    String PROP_VIEW_CATEGORY = SUBNODE_STYLE + "category.view";
    String PROP_EDIT_CATEGORY = SUBNODE_STYLE + "category.edit";
    String DEFAULT_VIEW_CATEGORY = "composum.pages.components.view";
    String DEFAULT_EDIT_CATEGORY = "composum.pages.components.edit";

    String SEARCH_PROPS = "search/";
    /** Property of a page that, if true, hides the page in search results. */
    String PROP_IGNORE_IN_SEARCH = SEARCH_PROPS + "ignoreInSearch";

    /** Site */

    String NODE_TYPE_SITE = CPP_PREFIX + "Site";
    String NODE_TYPE_SITE_CONFIGURATION = CPP_PREFIX + "SiteConfiguration";
    String PROP_HOMEPAGE = "homepage";
    String DEFAULT_HOMEPAGE_PATH = "home";

    /** Template */

    String PROP_TEMPLATE = "template";
    String PROP_IS_TEMPLATE = "isTemplate";
    String PROP_TEMPLATE_REF = "templateRef";

    String PROP_ALLOWED_PARENT_TEMPLATES = "allowedParentTemplates";
    String PROP_FORBIDDEN_PARENT_TEMPLATES = "forbiddenParentTemplates";
    String PROP_ALLOWED_CHILD_TEMPLATES = "allowedChildTemplates";
    String PROP_FORBIDDEN_CHILD_TEMPLATES = "forbiddenChildTemplates";

    String PROP_ALLOWED_PARENT_TYPES = "allowedParentTypes";
    String PROP_FORBIDDEN_PARENT_TYPES = "forbiddenParentTypes";
    String PROP_ALLOWED_CHILD_TYPES = "allowedChildTypes";
    String PROP_FORBIDDEN_CHILD_TYPES = "forbiddenChildTypes";

    String PROP_ALLOWED_PATHS = "allowedPaths";
    String PROP_FORBIDDEN_PATHS = "forbiddenPaths";

    /** Widget */

    String NODE_TYPE_WIDGET = CPP_PREFIX + "Widget";
    String PROP_WIDGET_TYPE = "widgetType";

    /** general properties */

    String PROP_CREATION_DATE = "jcr:created";
    String PROP_LAST_MODIFIED = ResourceUtil.PROP_LAST_MODIFIED;
    String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** */

    enum ComponentType {

        site, page, container, element, something;

        public static ComponentType typeOf(String string) {
            ComponentType type = something;
            if (StringUtils.isNotBlank(string)) {
                if (string.startsWith(CPP_PREFIX)) {
                    string = string.substring(4);
                }
                try {
                    type = valueOf(ComponentType.class, string.toLowerCase());
                } catch (IllegalArgumentException ex) {
                    // ok, something
                }
            }
            return type;
        }

        public static ComponentType typeOf(ResourceResolver resolver, Resource resource, String typeHint) {
            if (Site.isSite(resource)) {
                return site;
            } else if (Page.isPage(resource)) {
                return page;
            } else if (Container.isContainer(resolver, resource, typeHint)) {
                return container;
            } else if (Element.isElement(resolver, resource, typeHint)) {
                return element;
            } else {
                return something;
            }
        }

        public static String getPrimaryType(ComponentType type) {
            if (type != null) {
                switch (type) {
                    case site:
                        return NODE_TYPE_SITE;
                    case page:
                        return NODE_TYPE_PAGE;
                    case container:
                        return NODE_TYPE_CONTAINER;
                    case element:
                        return NODE_TYPE_ELEMENT;
                    default:
                        return NODE_TYPE_SOMETHING;
                }
            }
            return null;
        }
    }
}
