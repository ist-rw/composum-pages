package com.composum.pages.commons.replication;

import org.apache.sling.api.resource.Resource;

public interface ReplicationStrategy {

    boolean canReplicate(ReplicationContext context, Resource resource, boolean isReferenced);

    void replicate(ReplicationContext context, Resource resource, boolean recursive)
            throws Exception;

    void activate(ReplicationManager manager);

    void deactivate(ReplicationManager manager);
}
