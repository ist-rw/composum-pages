package com.composum.pages.commons.replication;

import com.composum.pages.commons.model.File;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(
        service = {ReplicationStrategy.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=In Place Asset Replication Strategy"
        },
        immediate = true
)
public class InPlaceAssetReplication extends InPlaceReplicationStrategy {

    /**
     * replicate asset references only not the complete asset collection
     */
    @Override
    public boolean canReplicate(ReplicationContext context, Resource resource, boolean isReferenced) {
        return isReferenced && File.isFile(resource);
    }

    @Override
    protected ResourceResolver getReleaseResolver(ReplicationContext context, Resource resource) {
        return context.getResolver();
    }
}
