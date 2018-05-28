package com.composum.pages.commons.replication;

import com.composum.pages.commons.model.Site;
import com.composum.sling.core.BeanContext;
import com.composum.sling.platform.security.AccessMode;

import java.util.HashSet;
import java.util.Set;

public class ReplicationContext extends BeanContext.Wrapper {

    public final Site site;
    public final AccessMode accessMode;

    public final Set<String> done = new HashSet<>();
    public final Set<String> references = new HashSet<>();

    public ReplicationContext(BeanContext beanContext, Site site, AccessMode accessMode) {
        super(beanContext);
        this.site = site;
        this.accessMode = accessMode;
    }
}
