package com.composum.pages.commons.model.properties;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.GenericModel;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.model.Page;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.SlingBean;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * a simple model for a subnode of a content element which declares a simple structured type
 * such a simple model has no embedded i18n functionality but the node can be used as i18n property
 */
public abstract class PropertyNode implements SlingBean {

    protected BeanContext context;
    protected Resource resource;
    protected ValueMap values;

    protected Model model;  // the model of the element which contains this structured property

    private transient Page currentPage;

    public PropertyNode() {
    }

    public PropertyNode(final BeanContext context, final Resource resource) {
        initialize(context, resource);
    }

    public Resource getResource() {
        return resource;
    }

    @Nonnull
    public String getName() {
        return resource.getName();
    }

    @Nonnull
    public String getPath() {
        return resource.getPath();
    }

    @Nonnull
    public String getType() {
        return resource.getResourceType();
    }

    /**
     * prepare the concrete type based on the resource and values...
     */
    protected void initialize() {
    }

    /**
     * the standard bean initializer
     */
    @Override
    public void initialize(final BeanContext context, final Resource resource) {
        this.context = context;
        this.resource = resource;
        values = resource == null || ResourceUtil.isNonExistingResource(resource)
                ? new ValueMapDecorator(new HashMap<>()) : resource.getValueMap();
        model = determineElementModel();
        initialize();
    }

    /**
     * the extension point to determine the element model of this property model
     */
    protected Model determineElementModel() {
        Resource modelResource = resource.getParent();
        // skip all unstructured parents to determine the element model for this property model
        while (modelResource != null && modelResource.isResourceType(JcrConstants.NT_UNSTRUCTURED)) {
            modelResource = modelResource.getParent();
        }
        if (modelResource != null) {
            return new GenericModel(context, modelResource);
        }
        return null;
    }

    @Override
    public void initialize(final BeanContext context) {
        initialize(context, context.getResource());
    }

    /**
     * the requested page referenced by the current HTTP request
     */
    @Nullable
    public Page getCurrentPage() {
        if (currentPage == null) {
            currentPage = context.getAttribute(PagesConstants.RA_CURRENT_PAGE, Page.class);
        }
        return currentPage;
    }
}
