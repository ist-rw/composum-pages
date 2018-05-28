package com.composum.pages.commons.request;

import com.composum.pages.commons.model.Site;
import com.composum.pages.commons.replication.ReplicationManager;
import com.composum.pages.commons.service.SiteManager;
import com.composum.sling.core.BeanContext;
import com.composum.sling.platform.security.AccessMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.composum.pages.commons.PagesConstants.COMPOSUM_PREFIX;
import static com.composum.pages.commons.PagesConstants.DISPLAY_MODE_ATTR;
import static com.composum.sling.platform.security.PlatformAccessFilter.ACCESS_MODE_KEY;
import static com.composum.sling.platform.security.PlatformAccessFilter.ACCESS_MODE_PREVIEW;
import static com.composum.sling.platform.security.PlatformAccessFilter.ACCESS_MODE_PUBLIC;
import static com.composum.sling.platform.staging.ResourceResolverChangeFilter.ATTRIBUTE_NAME;
import static com.composum.sling.platform.staging.ResourceResolverChangeFilter.COOKIE_NAME;
import static com.composum.sling.platform.staging.ResourceResolverChangeFilter.PARAMETER_NAME;

@Component(
        service = {Filter.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Platform Release Filter",
                "sling.filter.scope=REQUEST",
                "service.ranking:Integer=" + 5070
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = PagesReleaseFilter.Config.class)
public class PagesReleaseFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(PagesReleaseFilter.class);

    public static final String PUBLIC_RELEASE_LABEL = "public";

    public static final String ATTR_CACHE_DISABLED = "com.composum.platform.cache.component.ComponentCacheService#cacheDisabled";

    @ObjectClassDefinition(
            name = "Composum Platform Release Filter Configuration"
    )
    @interface Config {

        @AttributeDefinition(
                name = "release.filter.enabled",
                description = "the on/off switch for the Release Filter"
        )
        boolean enabled() default true;

        @AttributeDefinition(
                name = "unreleased.host.allow",
                description = "the hostname patterns for general (unreleased) artifacts"
        )
        String[] ignoredHostPatterns() default {};

        @AttributeDefinition(
                name = "unreleased.uri.allow",
                description = "the URI patterns for general (unreleased) artifacts"
        )
        String[] ignoredUriPatterns() default {
                "^/(apps|libs)/.*\\.(css|js|jpg|jpeg|gif|png|ttf|woff)$",
                "^/bin/public/clientlibs\\.(min\\.)?(css|js)(/.*)?$",
                "^/j_security_check$"
        };

        @AttributeDefinition(
                name = "unreleased.path.allow",
                description = "the path patterns for general (unreleased) artifacts"
        )
        String[] ignoredPathPatterns() default {"^/(apps|libs)/.*\\.(css|js|jpg|jpeg|gif|png|ttf|woff)$",
                "^/libs/sling/servlet/errorhandler/.*$",
                "^/libs/(themes|fonts|jslibs|composum|sling)/.*$",
                "^/libs(/composum/platform/security)?/login.*$"};
    }

    private PagesReleaseFilter.Config config;

    private List<Pattern> ignoredHostPatterns;

    private List<Pattern> ignoredUriPatterns;

    private List<Pattern> ignoredPathPatterns;

    @Reference
    protected SiteManager siteManager;

    @Reference
    protected ReplicationManager replicationManager;

    protected ServletContext servletContext;

    protected BundleContext bundleContext;

    /**
     * Determines the release to render depending on the access mode...
     * <dl>
     * <dt>'inPlace' staging mode is set for the requested site</dt>
     * <dd>is checking that the requested path matches to the replication path; rejects the request if this doesn't match</dd>
     * <dt>'versions' staging mode is set for the requested site</dt>
     * <dd>is setting the 'composum-platform-release-label' the the release label declared for the requested access mode</dd>
     * <dt>'live' staging mode is set for the requested site</dt>
     * <dd>no check, render author content as is as public content</dd>
     * </dl>
     */
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (config.enabled()) {
            String release = null;

            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            Resource resource = slingRequest.getResource();
            BeanContext context = new BeanContext.Servlet(servletContext, bundleContext, slingRequest,
                    (SlingHttpServletResponse) response);

            Site site = siteManager.getContainingSite(context, resource);

            String accessMode = (String) request.getAttribute(ACCESS_MODE_KEY);
            if (StringUtils.isNotBlank(accessMode) &&
                    !AccessMode.AUTHOR.name().equals(accessMode = accessMode.toUpperCase())) {

                // not an AUTHOR access; check public/preview access against the staging policy of the site...

                String uri = ((SlingHttpServletRequest) request).getRequestURI();
                String contextPath = slingRequest.getContextPath();
                if (uri.startsWith(contextPath)) {
                    uri = uri.substring(contextPath.length());
                }
                String path = resource.getPath();

                if (!isUnreleasedPublicAccessAllowed(request.getServerName(), uri, path)) {

                    if (site != null) {
                        String mode = site.getPublicMode();
                        switch (mode) {
                            case Site.PUBLIC_MODE_IN_PLACE:
                                switch (accessMode) {
                                    case ACCESS_MODE_PUBLIC:
                                        if (!path.startsWith(replicationManager.getConfig().inPlacePublicPath())) {
                                            sendReject(response, "not a public resource request", uri, resource);
                                            return;
                                        }
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("inPlace public: '{}' ({})", site.getPath(), resource);
                                        }
                                        break;
                                    case ACCESS_MODE_PREVIEW:
                                        if (!path.startsWith(replicationManager.getConfig().inPlacePreviewPath())) {
                                            sendReject(response, "not a preview resource request", uri, resource);
                                            return;
                                        }
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("inPlace preview: '{}' ({})", site.getPath(), resource);
                                        }
                                        break;
                                }
                                break;
                            case Site.PUBLIC_MODE_VERSIONS:
                                release = site.getReleaseLabel(accessMode);
                                if (StringUtils.isBlank(release)) {
                                    sendReject(response, "no appropriate release found", uri, resource);
                                    return;
                                }
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("release: '{}' ({})", release + '@' + site.getPath(), resource);
                                }
                                break;
                        }
                    } else {
                        sendReject(response, "no appropriate site found", uri, resource);
                        return;
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("unreleased: '{}' ({})", uri, resource);
                    }
                }

            } else {

                // if Author or unspecific mode use requested release...

                release = request.getParameter(PARAMETER_NAME);
                if (StringUtils.isBlank(release)) {
                    Cookie cookie = slingRequest.getCookie(COOKIE_NAME);
                    if (cookie != null) {
                        release = cookie.getValue();
                    }
                    if (StringUtils.isBlank(release)) {
                        HttpSession session = slingRequest.getSession(false);
                        if (session != null) {
                            release = (String) session.getAttribute(ATTRIBUTE_NAME);
                        }
                    }
                }

                if (StringUtils.isNotBlank(release)) {
                    if (Character.isDigit(release.charAt(0))) {
                        // use release labe as is if stating with a digit
                        release = Site.RELEASE_LABEL_PREFIX + release;
                    } else {
                        if (site != null && !release.startsWith(COMPOSUM_PREFIX)) {
                            // assuming that an access mode is set for the requested release
                            String mapped = site.getReleaseLabel(release);
                            if (StringUtils.isNotBlank(mapped)) {
                                release = mapped;
                            }
                        }
                    }
                }
            }

            if (StringUtils.isNotBlank(release)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} access mode, release '{}' selected", accessMode, release);
                }
                request.setAttribute(ATTRIBUTE_NAME, release);
                // disable editing for release requests
                slingRequest.adaptTo(DisplayMode.class).reset(DisplayMode.Value.NONE);
            }

            // disable component caching if in edit context ('edit', 'preview', 'browse', 'develop')
            if (DisplayMode.isEditMode(context) || DisplayMode.isPreviewMode(context)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("EDIT mode, no cache for '{}'", resource.getPath());
                }
                slingRequest.setAttribute(ATTR_CACHE_DISABLED, Boolean.TRUE);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("chain.doFilter()...");
        }
        chain.doFilter(request, response);
    }

    protected boolean isUnreleasedPublicAccessAllowed(String hostname, String uri, String path) {
        for (Pattern pattern : ignoredHostPatterns) {
            if (pattern.matcher(hostname).matches()) {
                return true;
            }
        }
        for (Pattern pattern : ignoredUriPatterns) {
            if (pattern.matcher(uri).matches()) {
                return true;
            }
        }
        for (Pattern pattern : ignoredPathPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    protected void sendReject(ServletResponse response, String message, String uri, Resource resource)
            throws IOException {
        HttpServletResponse slingResponse = (HttpServletResponse) response;
        LOG.warn("REJECT(release): '" + uri + "' - " + message + " (" + resource + ").");
        slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    public void init(FilterConfig filterConfig) {
        servletContext = filterConfig.getServletContext();
    }

    public void destroy() {
    }

    @Activate
    @Modified
    public void activate(final PagesReleaseFilter.Config config) {
        this.config = config;
        ignoredHostPatterns = new ArrayList<>();
        for (String rule : PropertiesUtil.toStringArray(config.ignoredHostPatterns())) {
            if (StringUtils.isNotBlank(rule = rule.trim())) ignoredHostPatterns.add(Pattern.compile(rule));
        }
        ignoredUriPatterns = new ArrayList<>();
        for (String rule : PropertiesUtil.toStringArray(config.ignoredUriPatterns())) {
            if (StringUtils.isNotBlank(rule = rule.trim())) ignoredUriPatterns.add(Pattern.compile(rule));
        }
        ignoredPathPatterns = new ArrayList<>();
        for (String rule : PropertiesUtil.toStringArray(config.ignoredPathPatterns())) {
            if (StringUtils.isNotBlank(rule = rule.trim())) ignoredPathPatterns.add(Pattern.compile(rule));
        }
    }
}
