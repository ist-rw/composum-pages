package com.composum.pages.commons.replication;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component(
        service = {ReplicationManager.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Pages Replication Manager"
        },
        immediate = true
)
@Designate(ocd = PagesReplicationConfig.class)
public class PagesReplicationManager implements ReplicationManager {

    private static final Logger LOG = LoggerFactory.getLogger(PagesReplicationManager.class);

    protected PagesReplicationConfig config;

    protected List<ReplicationStrategy> instances = Collections.synchronizedList(new ArrayList<ReplicationStrategy>());

    @Override
    public PagesReplicationConfig getConfig() {
        return config;
    }

    @Override
    public void replicateResource(ReplicationContext context, Resource resource, boolean recursive)
            throws Exception {
        replicateResource(context, resource, false, recursive);
    }

    @Override
    public void replicateReferences(ReplicationContext context)
            throws Exception {
        int maxLoops = 5;
        do {
            String[] references = context.references.toArray(new String[0]);
            context.references.clear();
            ResourceResolver resolver = context.getResolver();
            for (String referencedPath : references) {
                Resource resource = resolver.getResource(referencedPath);
                if (resource != null) {
                    replicateResource(context, resource, true, false);
                } else {
                    LOG.warn("can't resolve reference '{}'", referencedPath);
                }
            }
        } while (--maxLoops > 0 && context.references.size() > 0);
    }

    protected void replicateResource(ReplicationContext context, Resource resource,
                                     boolean isReference, boolean recursive)
            throws Exception {
        boolean replicationDone = false;
        for (ReplicationStrategy strategy : instances) {
            if (strategy.canReplicate(context, resource, isReference)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("replicate" + (isReference ? " reference" : "") + " '{}' using {}",
                            resource.getPath(), strategy.getClass().getSimpleName());
                }
                strategy.replicate(context, resource, recursive);
                replicationDone = true;
            }
        }
        if (!replicationDone && isReference) {
            LOG.warn("referenced resource '{}' not replicated", resource.getPath());
        }
    }

    @Reference(service = ReplicationStrategy.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addReplicationStrategy(@Nonnull final ReplicationStrategy strategy) {
        LOG.info("addReplicationStrategy: {}", strategy.getClass().getSimpleName());
        if (config != null) {
            strategy.activate(this);
        }
        instances.add(strategy);
    }

    protected void removeReplicationStrategy(@Nonnull final ReplicationStrategy strategy) {
        LOG.info("removeReplicationStrategy: {}", strategy.getClass().getSimpleName());
        instances.remove(strategy);
    }

    @Activate
    @Modified
    public void activate(PagesReplicationConfig config) {
        this.config = config;
        for (ReplicationStrategy strategy : instances) {
            strategy.activate(this);
        }
    }

    @Deactivate
    public void deactivate() {
        for (ReplicationStrategy strategy : instances) {
            strategy.deactivate(this);
        }
    }
}