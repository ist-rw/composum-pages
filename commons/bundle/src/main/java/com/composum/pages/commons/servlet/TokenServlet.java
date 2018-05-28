package com.composum.pages.commons.servlet;

import com.composum.pages.commons.service.TrackingService;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;

/**
 * the TokenServlet delegates each request to the 'TrackingService' to track the usage of the current
 * content; the TokenServlet is delivering a simple 1x1 transparent image after the request tracking
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Token Servlet",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=token",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=png",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default"
        })
public class TokenServlet extends SlingSafeMethodsServlet {

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TokenServlet.class);

    public static final byte[] TRACKING_PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0xDA, 0x63, 0x60, 0x00, 0x02,
            0x00, 0x00, 0x05, 0x00, 0x01, (byte) 0xE9, (byte) 0xFA, (byte) 0xDC, (byte) 0xD8, 0x00, 0x00, 0x00, 0x00,
            0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @Reference
    protected TrackingService trackingService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request,
                         @Nonnull SlingHttpServletResponse response) {

        response.setStatus(HttpServletResponse.SC_OK);
        AbstractServiceServlet.setNoCacheHeaders(response);

        try {
            String uri = request.getRequestURI();
            Resource resource = request.getResource();
            if (!ResourceUtil.isNonExistingResource(resource)) {
                try {
                    String path = resource.getPath();
                    String suffix = request.getRequestPathInfo().getSuffix();
                    String referer = null;
                    if (StringUtils.isNotBlank(suffix)) {
                        referer = suffix.substring(1);
                        // the base64 encoded referer is NOT decoded here (the encoded string is used as a node name)
                    }
                    trackingService.trackToken(
                            new BeanContext.Servlet(getServletContext(), bundleContext, request, response),
                            path,
                            referer);
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            } else {
                LOG.warn("resource not found: " + uri);
            }
            response.setContentType("image/png");
            response.setContentLength(TRACKING_PNG.length);
            IOUtils.copy(new ByteArrayInputStream(TRACKING_PNG), response.getOutputStream());

        } catch (Throwable ex) {
            // no exception message sent back to the client...
            LOG.error(ex.getMessage(), ex);
        }
    }
}
