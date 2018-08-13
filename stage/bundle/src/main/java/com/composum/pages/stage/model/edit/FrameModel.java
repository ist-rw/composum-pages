package com.composum.pages.stage.model.edit;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.Component;
import com.composum.pages.commons.model.GenericModel;
import com.composum.pages.commons.util.ResolverUtil;
import com.composum.pages.commons.util.ResourceTypeUtil;
import com.composum.pages.commons.util.TagCssClasses;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import static com.composum.pages.commons.model.AbstractModel.CSS_BASE_TYPE_RESTRICTION;
import static com.composum.pages.commons.util.ResourceTypeUtil.EDIT_TILE_PATH;
import static com.composum.sling.core.servlet.AbstractServiceServlet.PARAM_TYPE;

/**
 * a model of a frame component which is an editing component of an element; this model is a wrapper for the element to edit
 */
public class FrameModel extends GenericModel {

    private transient Resource delegateType;
    private transient PagesConstants.ComponentType componentType;

    public PagesConstants.ComponentType getComponentType() {
        if (componentType == null) {
            BeanContext context = delegate.getContext();
            SlingHttpServletRequest request = context.getRequest();
            // use type hint if available (useful on type overriding during 'include')
            String typeParam = request.getParameter(PARAM_TYPE);
            componentType = PagesConstants.ComponentType.typeOf(context.getResolver(), getResource(), typeParam);
        }
        return componentType;
    }

    /**
     * @return the resource of the frame element itself instead of the element to edit
     */
    public Resource getFrameResource() {
        return getContext().getResource();
    }

    /**
     * @return the CSS base of the frame element itself not of the delegate
     */
    @Override
    public String getCssBase() {
        Resource resource = getFrameResource();
        String type = CSS_BASE_TYPE_RESTRICTION.accept(resource) ? resource.getResourceType() : null;
        return StringUtils.isNotBlank(type) ? TagCssClasses.cssOfType(type) : null;
    }

    /**
     * retrieves the element resource focused by the frame element for editing
     */
    @Override
    protected Resource determineDelegateResource(BeanContext context, Resource resource) {
        String path = getDelegatePath(context);
        return context.getResolver().resolve(path);
    }

    /**
     * retrieves the path of the element to handle by the frame element using the suffix of the request
     */
    public static String getDelegatePath(BeanContext context) {
        SlingHttpServletRequest request = context.getRequest();
        String delegatePath = request.getRequestPathInfo().getSuffix();
        if (StringUtils.isBlank(delegatePath)) {
            delegatePath = "/";
        } else {
            Resource resource = request.getResourceResolver().resolve(delegatePath);
            if (!ResourceUtil.isNonExistingResource(resource)) {
                delegatePath = resource.getPath();
            } else {
                if (delegatePath.endsWith(".html")) {
                    delegatePath = delegatePath.substring(0, delegatePath.length() - 5);
                }
            }
        }
        return delegatePath;
    }

    public Resource getTypeResource() {
        if (delegateType == null) {
            Resource resource = getResource();
            delegateType = ResolverUtil.getResourceType(resource, resource.getResourceType());
            if (delegateType == null) {
                BeanContext context = delegate.getContext();
                String type = context.getRequest().getParameter(Component.TYPE_HINT_PARAM);
                if (StringUtils.isBlank(type)) {
                    delegateType = ResolverUtil.getResourceType(resource, type);
                }
            }
        }
        return delegateType;
    }

    public String getTypePath() {
        Resource type = getTypeResource();
        return type != null ? type.getPath() : "";
    }

    // Tile rendering

    public String getTileResourceType() {
        return ResourceTypeUtil.getSubtypePath(getContext().getResolver(), getResource(), getPath(), EDIT_TILE_PATH, null);
    }
}