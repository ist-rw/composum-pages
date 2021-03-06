/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.pages.commons.taglib;

import com.composum.pages.commons.model.Container;
import com.composum.pages.commons.model.Element;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.util.TagCssClasses;
import com.composum.sling.core.request.DomIdentifiers;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.io.IOException;
import java.util.Map;

import static com.composum.sling.cpnl.TagBase.TAG_NONE;

/**
 * the tag to render a Pages Sling component
 * such a component is rendering an HTML tag with the components content within
 * the tag around contains all additional information for the edit tools if edit mode is set
 */
public class ElementTag extends AbstractWrappingTag {

    public static final String DEFAULT_TAG = "div";

    public static final String COMPONENT_EDIT_BODY_CLASSES = "composum-pages-component";
    public static final String ELEMENT_EDIT_CSS_CLASS = "composum-pages-element";

    public static final String STYLE_PROPERTY = "style";

    protected String id;
    protected String tagId;
    protected String tagName;
    protected String tagNameValue;
    protected String tagAttributes;
    protected String tagAttributesValue;
    protected DisplayMode.Value displayMode;

    @Override
    protected void clear() {
        displayMode = null;
        tagAttributesValue = null;
        tagAttributes = null;
        tagNameValue = null;
        tagName = null;
        tagId = null;
        id = null;
        super.clear();
    }

    /**
     * the optional DOM tree id for the HTML tag
     */
    public String getTagId() {
        return tagId;
    }

    public void setTagId(String id) {
        tagId = id;
    }

    public String getId() {
        if (id == null) {
            id = getTagId();
            if (StringUtils.isNotBlank(id)) {
                id = eval(id, id);
            }
        }
        return id;
    }

    /**
     * the tag name to render the wrapping tag (default: 'div')
     */
    public String getTagName() {
        if (tagNameValue == null) {
            tagNameValue = eval(tagName, tagName);
        }
        return tagNameValue;
    }

    public void setTagName(String name) {
        tagName = name;
    }

    public String getTagAttributes() {
        if (tagAttributesValue == null) {
            tagAttributesValue = eval(tagAttributes, tagAttributes);
        }
        return tagAttributesValue;
    }

    public void setTagAttributes(String attributes) {
        tagAttributes = attributes;
    }

    public boolean isWithTag() {
        return !TAG_NONE.equalsIgnoreCase(getTagName());
    }

    /**
     * the edit mode for this render step and all included components
     */
    @Override
    public void setMode(String mode) {
        super.displayMode = displayMode = DisplayMode.Value.valueOf(mode.toUpperCase());
    }

    /**
     * the edit CSS class for this component (normally 'element'; extension hook used by the container tag)
     */
    protected String getElementCssClass() {
        return ELEMENT_EDIT_CSS_CLASS;
    }

    /**
     * builds the list of CSS classes for the wrapping tag
     */
    protected void collectCssClasses(TagCssClasses.CssSet collection) {
        ValueMap values = resource.adaptTo(ValueMap.class);
        if (values != null) {
            collection.add(values.get(STYLE_PROPERTY, ""));
        }
        super.collectCssClasses(collection);
        if (isEditMode()) {
            collection.add(COMPONENT_EDIT_BODY_CLASSES);
            collection.add(getElementCssClass());
        }
    }

    /**
     * builds the list of tag attributes for the wrapping tag
     */
    @Override
    protected void collectAttributes(Map<String, Object> attributeSet) {
        String value;
        if (StringUtils.isNotBlank(value = getId())) {
            attributeSet.put(TAG_ID, value);
        }
        super.collectAttributes(attributeSet);
        if (isEditMode()) {
            attributeSet.put(PAGES_EDIT_DATA_PATH, resource.getPath());
            addEditAttributes(attributeSet, resource, resource.getResourceType());
            if (isDraggable()) {
                attributeSet.put("draggable", "true");
            }
        }
    }

    @Override
    public String getAttributes() {
        String attributes = super.getAttributes();
        String tagAttrs = getTagAttributes();
        if (StringUtils.isNotBlank(tagAttrs)) {
            attributes += tagAttrs.startsWith(" ") ? tagAttrs : " " + tagAttrs;
        }
        return attributes;
    }

    // hierarchy

    protected boolean isDraggable() {
        return getContainer() != null;
    }

    protected Resource getContainer() {
        Resource parent = resource.getParent();
        while (parent != null) {
            if (Container.isContainer(resourceResolver, parent, null)) {
                // if parent is a container this parent is that we are searching for
                // if we itself are a dynamic element of the container
                Element element = new Element(context, resource);
                Container container = new Container(context, parent);
                return container.isAllowedElement(element) && element.isAllowedContainer(container)
                        ? parent  // embedded dynamically
                        : null;   // placed inside by a static include
            }
            if (Element.isElement(resourceResolver, parent, null) || Page.isPageContent(parent) || Page.isPage(parent)) {
                // if parent is an element or page is reached the resource itself is not a container element
                return null;
            }
            parent = parent.getParent();
        }
        return null;
    }

    // rendering

    /**
     * setup before the rendering starts - sets the display mode if specified
     */
    @Override
    protected void prepareTagStart() {
        String var = getVar();
        if (displayMode != null) {
            DisplayMode.get(context).push(displayMode);
        }
        if (StringUtils.isBlank(getTagName())) {
            tagNameValue = DEFAULT_TAG;
        }
        Model model = (Model) getModel();
        if (model != null) {
            setAttribute(var + "Id",
                    DomIdentifiers.getInstance(context).getElementId(model),
                    getVarScope());
        }
    }

    /**
     * renders the tag start HTML element if not 'none' is set for the tag name (tagName='none')
     * if 'none' is set the content is rendered only not the wrapping tag (with no edit capability)
     */
    @Override
    protected void renderTagStart() throws IOException {
        if (isWithTag()) {
            out.append("<").append(getTagName()).append(getAttributes()).append(">\n");
        }
    }

    /**
     * renders the tag end HTML element if not 'none' is set for the tag name
     */
    @Override
    protected void renderTagEnd() throws IOException {
        if (isWithTag()) {
            out.append("</").append(getTagName()).append(">\n");
        }
    }

    /**
     * cleanup after rendering ends - resets the display mode if changed
     */
    @Override
    protected void finishTagEnd() {
        if (displayMode != null) {
            DisplayMode.get(context).pop();
        }
    }
}
