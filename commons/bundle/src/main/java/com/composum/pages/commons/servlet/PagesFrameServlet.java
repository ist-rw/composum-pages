package com.composum.pages.commons.servlet;

import com.composum.pages.commons.request.DisplayMode;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractConsoleServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.util.regex.Pattern;

import static com.composum.pages.commons.PagesConstants.DISPLAY_MODE_ATTR;
import static com.composum.pages.commons.PagesConstants.DISPLAY_MODE_REQ_PARAM;
import static com.composum.pages.commons.PagesConstants.FRAME_CONTEXT_ATTR;
import static com.composum.pages.commons.PagesConstants.PAGES_LOCALE_ATTR;

/**
 * The general hook (servlet) for the Pages edit stage; provides the path '/bin/pages.html/...'.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Frame Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/pages",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
public class PagesFrameServlet extends AbstractConsoleServlet {

    public static final String RESOURCE_TYPE = "composum/pages/stage/{mode}/frame";

    public static final String CONSOLE_PATH = "/libs/composum/pages/stage/edit/console/content";

    public static final Pattern PATH_PATTERN = Pattern.compile("^(/bin/pages(\\.[^/]+)?\\.html)(/.*)?$");

    protected Pattern getPathPattern(BeanContext context) {
        return PATH_PATTERN;
    }

    /**
     * Adds some hints that this request is a 'Pages Frame' request; used to control the 'persistence' of
     * the selected display mode and selected language (locale)
     */
    @Override
    protected BeanContext createContext(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        BeanContext context = super.createContext(request, response);
        context.setAttribute(FRAME_CONTEXT_ATTR + ":" + DISPLAY_MODE_ATTR,
                Boolean.TRUE, BeanContext.Scope.request);
        context.setAttribute(FRAME_CONTEXT_ATTR + ":" + PAGES_LOCALE_ATTR,
                Boolean.TRUE, BeanContext.Scope.request);
        return context;
    }

    /**
     * Returns the resource type for the Pages stage frame according to the defined display mode.
     */
    protected String getResourceType(BeanContext context) {
        SlingHttpServletRequest request = context.getRequest();
        DisplayMode.Value mode = DisplayMode.current(context);
        if (DisplayMode.Value.NONE == mode &&
                !DisplayMode.Value.NONE.name().equalsIgnoreCase(
                        request.getParameter(DISPLAY_MODE_REQ_PARAM))) {
            // replace stored 'NONE' by 'EDIT' if this 'Pages' servlet is used and store the new mode
            mode = DisplayMode.Value.EDIT;
            request.adaptTo(DisplayMode.class).reset(mode);
        }
        switch (mode) {
            case DEVELOP: // no extra view for the 'develop' mode
                mode = DisplayMode.Value.EDIT;
                break;
        }
        return RESOURCE_TYPE.replaceAll("\\{mode}", mode.name().toLowerCase());
    }

    @Override
    protected String getConsolePath(BeanContext context) {
        return CONSOLE_PATH;
    }
}