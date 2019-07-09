package com.composum.pages.stage.model.edit;

import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.util.LinkUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;

public class FramePage extends Page {

    private transient Page page;
    private transient Resource pageResource;
    private transient String pagePath;

    private transient DisplayMode.Value displayMode;

    public FramePage() {
        super();
    }

    /**
     * @return the resource of the edited page (this is not the initializers resource)
     */
    @Nonnull
    @Override
    public Resource getResource() {
        return getPageResource();
    }

    /**
     * @return the initializers resource (the edit frame component resource)
     */
    public Resource getFrameResource() {
        return super.getResource();
    }

    public Page getPage() {
        if (page == null) {
            page = getPageManager().createBean(context, getPageResource());
        }
        return page;
    }

    /**
     * @return the resource of the edited page
     */
    public Resource getPageResource() {
        if (pageResource == null) {
            String path = getPagePath();
            if (pageResource == null) {
                pageResource = resolver.resolve(path);
            }
        }
        return pageResource;
    }

    /**
     * @return the path of the edited page
     */
    public String getPagePath() {
        if (pagePath == null) {
            pagePath = context.getRequest().getRequestPathInfo().getSuffix();
            if (StringUtils.isBlank(pagePath)) {
                pagePath = "/";
            } else {
                pageResource = resolver.resolve(pagePath);
                if (!ResourceUtil.isNonExistingResource(pageResource)) {
                    pagePath = pageResource.getPath();
                } else {
                    if (pagePath.endsWith(".html")) {
                        pagePath = pagePath.substring(0, pagePath.length() - 5);
                    }
                }
            }
        }
        return pagePath;
    }

    /**
     * @return the URL of the edited page
     */
    public String getPageUrl() {
        return LinkUtil.getUrl(context.getRequest(), getPagePath());
    }

    /**
     * send a redirect to the edited page as response of the current context
     */
    public void redirectToPage() throws IOException {
        context.getResponse().sendRedirect(getPageUrl());
    }

    public boolean isHasLanguageVariations() {
        return getPage().getPageLanguages().getLanguages().size() > 1;
    }

    // view mode

    public boolean isEditMode() {
        DisplayMode.Value mode = getDisplayMode();
        return mode == DisplayMode.Value.EDIT || mode == DisplayMode.Value.DEVELOP;
    }

    public boolean isDevelopMode() {
        return getDisplayMode() == DisplayMode.Value.DEVELOP;
    }

    /**
     * @return the requested display mode (the current could be overloaded during 'include')
     */
    public DisplayMode.Value getDisplayMode() {
        if (displayMode == null) {
            displayMode = DisplayMode.requested(getContext());
        }
        return displayMode;
    }

    /**
     * @return a readable string of the current display mode
     */
    public String getDisplayModeHint() {
        DisplayMode.Value mode = getDisplayMode();
        switch (mode) {
            case DEVELOP:
                mode = DisplayMode.Value.EDIT;
            default:
                break;
        }
        return mode.name();
    }

    /**
     * @return the Sling selector key for the current display mode
     */
    public String getDisplayModeSelector() {
        return getDisplayModeHint().toLowerCase();
    }
}
