package com.composum.pages.commons.request;

import com.composum.sling.platform.security.AccessMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.composum.pages.commons.PagesConstants.ACCESS_MODE_ATTR;
import static com.composum.pages.commons.PagesConstants.ACCESS_MODE_REQ_PARAM;
import static com.composum.pages.commons.PagesConstants.DISPLAY_MODE_ATTR;
import static com.composum.pages.commons.PagesConstants.DISPLAY_MODE_REQ_PARAM;
import static com.composum.pages.commons.PagesConstants.LOCALE_REQUEST_PARAM;
import static com.composum.pages.commons.PagesConstants.PAGES_LOCALE_ATTR;

/**
 * the adapter factory for all aspects in the request context of the Pages module
 * <p>
 * <dl>
 * <dt>access mode</dt>
 * <dd>the access mode is normally set by the platform access filter an can be overridden by the
 * parameter 'pages.access' with the values 'AUTHOR', 'PREVIEW' or 'PUBLIC'
 * if the request is received via an authoring host</dd>
 * <dt>display mode</dt>
 * <dd>the display mode can be specified by the parameter 'pages.mode' with the
 * values 'NONE', 'EDIT', 'PREVIEW', 'BROWSE' or 'DEVELOP'
 * if the request is received via an authoring host</dd>
 * <dt>locale (language)</dt>
 * <dd>the locale is specified by the parameter 'pages.locale' with
 * normal Locale values, e.g. 'en_EN_US', 'de_DE', 'fr'</dd>
 * </dl>
 * Each aspect value specified by a parameter is kept in the session and reused in further request.
 * </p>
 */
@Component(name = "Composum Pages Request Aspects Adapter Factory",
        property = {
                Constants.SERVICE_DESCRIPTION + "=the adapter factory for all request aspects - access mode, display mode, language",
                AdapterFactory.ADAPTER_CLASSES + "=com.composum.sling.platform.security.AccessMode",
                AdapterFactory.ADAPTER_CLASSES + "=com.composum.pages.commons.request.DisplayMode",
                AdapterFactory.ADAPTER_CLASSES + "=java.util.Locale",
                AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.SlingHttpServletRequest"
        })
public class RequestAspectAdapterFactory implements AdapterFactory {

    public static final String REQUEST_ASPECTS_PREFIX = "pages_request_aspect#";

    protected static final Map<String, AspectFactory> factoryMap;

    static {
        factoryMap = new HashMap<>();
        factoryMap.put(AccessMode.class.getName(), new AccessModeFactory());
        factoryMap.put(DisplayMode.class.getName(), new DisplayModeFactory());
        factoryMap.put(Locale.class.getName(), new LocaleFactory());
    }

    @Override
    public <AdapterType> AdapterType getAdapter(@Nonnull Object adaptable, @Nonnull Class<AdapterType> type) {
        AdapterType result = null;
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;
            String typeName = type.getName();
            AspectFactory factory = factoryMap.get(typeName);
            String key = factory.getKey();
            result = type.cast(request.getAttribute(key));
            if (result == null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    try {
                        result = type.cast(factory.getFromSession(session, key));
                    } catch (ClassCastException ccex) {
                        // reset to new instance
                    }
                }
                if (result == null) {
                    result = type.cast(factory.createFromRequest(request, key));
                    if (result != null) {
                        session = request.getSession(true);
                        factory.keepInSession(session, key, result);
                        request.setAttribute(key, result);
                    }
                }
            }
        }
        return result;
    }

    protected interface AspectFactory<Type> {

        String getKey();

        Type createFromRequest(SlingHttpServletRequest request, String key);

        Type getFromSession(HttpSession session, String key);

        void keepInSession(HttpSession session, String key, Object value);
    }

    protected static class AccessModeFactory implements AspectFactory<AccessMode> {

        @Override
        public String getKey() {
            return ACCESS_MODE_ATTR;
        }

        @Override
        public AccessMode createFromRequest(SlingHttpServletRequest request, String key) {
            AccessMode accessMode = AccessMode.PUBLIC;
            String parameter = request.getParameter(ACCESS_MODE_REQ_PARAM);
            if (StringUtils.isNotBlank(parameter)) {
                accessMode = AccessMode.valueOf(parameter.trim().toUpperCase());
            }
            return accessMode;
        }

        @Override
        public AccessMode getFromSession(HttpSession session, String key) {
            return (AccessMode) session.getAttribute(key);
        }

        @Override
        public void keepInSession(HttpSession session, String key, Object value) {
            session.setAttribute(key, value);
        }
    }

    protected static class DisplayModeFactory implements AspectFactory<DisplayMode> {

        @Override
        public String getKey() {
            return DISPLAY_MODE_ATTR;
        }

        @Override
        public DisplayMode createFromRequest(SlingHttpServletRequest request, String key) {
            DisplayMode.Value value = DisplayMode.Value.EDIT;
            String parameter = request.getParameter(DISPLAY_MODE_REQ_PARAM);
            if (StringUtils.isNotBlank(parameter)) {
                value = DisplayMode.Value.valueOf(parameter.trim().toUpperCase());
            } else {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    try {
                        value = (DisplayMode.Value) session.getAttribute(key);
                    } catch (ClassCastException ccex) {
                        // reset to new instance
                    }
                }
            }
            return new DisplayMode(value);
        }

        @Override
        public DisplayMode getFromSession(HttpSession session, String key) {
            return null;
        }

        @Override
        public void keepInSession(HttpSession session, String key, Object value) {
            session.setAttribute(key, ((DisplayMode) value).firstElement());
        }
    }

    protected static class LocaleFactory implements AspectFactory<Locale> {

        @Override
        public String getKey() {
            return PAGES_LOCALE_ATTR;
        }

        @Override
        public Locale createFromRequest(SlingHttpServletRequest request, String key) {
            Locale locale = request.getLocale();
            String parameter = request.getParameter(LOCALE_REQUEST_PARAM);
            if (StringUtils.isNotBlank(parameter)) {
                locale = getLocale(parameter, locale);
            }
            return locale;
        }

        @Override
        public Locale getFromSession(HttpSession session, String key) {
            return (Locale) session.getAttribute(key);
        }

        @Override
        public void keepInSession(HttpSession session, String key, Object value) {
            session.setAttribute(key, value);
        }

        /**
         * transforms a string value into the best matching Locale instance
         */
        public Locale getLocale(String rule, Locale defaultValue) {
            Locale locale = defaultValue;
            if (StringUtils.isNotBlank(rule)) {
                rule = rule.trim().replaceAll(" +", "_").replaceAll("-", "_");
                String[] values = StringUtils.split(rule.trim(), '_');
                switch (values.length) {
                    case 0:
                        break;
                    case 1:
                        locale = new Locale(values[0]);
                        break;
                    case 2:
                        locale = new Locale(values[0], values[1]);
                        break;
                    default:
                        locale = new Locale(values[0], values[1], values[2]);
                        break;
                }
            }
            return locale;
        }
    }
}
