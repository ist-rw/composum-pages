package com.composum.pages.commons.replication;

import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.Site;
import com.composum.pages.commons.service.SiteManager;
import com.composum.sling.platform.staging.StagingResourceResolver;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {ReplicationStrategy.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=In Place Page Replication Strategy"
        },
        immediate = true
)
public class InPlacePageReplication extends InPlaceReplicationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(InPlacePageReplication.class);

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Reference
    protected SiteManager siteManager;

    @Override
    public boolean canReplicate(ReplicationContext context, Resource resource, boolean isReferenced) {
        boolean result = canReplicateSite(context, resource);
        if (result) {
            if (Page.isPage(resource)) {
                // the content of a page must be available in the staging resolvers context (version available)
                ResourceResolver releaseResolver = getReleaseResolver(context, resource);
                Resource released = releaseResolver.getResource(resource.getPath());
                result = released != null &&
                        (Page.isPage(released) && released.getChild(JcrConstants.JCR_CONTENT) != null);
            } else {
                result = (Site.isSite(resource));
            }
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("canReplicate({}): {}", resource.getPath(), result);
        }
        return result;
    }

    @Override
    protected ResourceResolver getReleaseResolver(ReplicationContext context, Resource resource) {
        ResourceResolver defaultResolver = context.getResolver();
        if (Page.isPage(resource)) {
            // for a page the version based resolver has to be used...
            String releaseLabel = context.site.getReleaseLabel(context.accessMode.name());
            if (LOG.isDebugEnabled()) {
                LOG.debug("using staging resolver of release '{}'...", releaseLabel);
            }
            return new StagingResourceResolver(resolverFactory, defaultResolver, releaseLabel, this);
        }
        return defaultResolver;
    }
}
