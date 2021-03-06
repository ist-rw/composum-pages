package com.composum.pages.commons.taglib;

import com.composum.pages.commons.service.WidgetManager;
import com.composum.sling.core.BeanContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import static com.composum.pages.commons.taglib.EditDialogGroupTag.DIALOG_GROUP_VAR;
import static com.composum.pages.commons.taglib.EditDialogTabTag.DIALOG_TAB_VAR;

/**
 * the EditWidgetTag is rendering a dialog widget as an element of the edit dialog form
 */
public abstract class AbstractWidgetTag extends AbstractFormTag {

    public static final String PROPERTY_RESOURCE_ATTR = "propertyResource";
    public static final String PROPERTY_PATH_ATTR = "propertyPath";

    protected String label;
    protected String property;
    protected String name;
    protected boolean i18n = false;
    protected String modelClass;

    private transient String relativePath;
    private transient String propertyName;

    @Override
    protected void clear() {
        propertyName = null;
        relativePath = null;
        modelClass = null;
        i18n = false;
        name = null;
        property = null;
        label = null;
        super.clear();
    }

    public boolean getHasLabel() {
        return label != null;
    }

    public String getLabel() {
        return i18n(label);
    }

    public void setLabel(String text) {
        label = text;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String name) {
        property = name;
    }

    public boolean isI18n() {
        return i18n;
    }

    public void setI18n(boolean val) {
        i18n = val;
    }

    public String getModelClass() {
        return modelClass;
    }

    public void setModelClass(String className) {
        modelClass = className;
    }

    public boolean isFormWidget() {
        return name == null || !name.startsWith("#");
    }

    /**
     * @return the name of the form input element probably with a prepended relative path and the i18n path
     */
    public String getName() {
        return getPropertyName();
    }

    public void setName(String key) {
        name = key;
    }

    public void setDisabled(boolean value) {
        disabled = value;
    }

    public boolean isDisabled() {
        Boolean result = null;
        if (hasDisabledAttribute()) {
            result = getDisabledValue();
        } else {
            EditDialogGroupTag groupTag = (EditDialogGroupTag) pageContext.findAttribute(DIALOG_GROUP_VAR);
            if (groupTag != null && groupTag.hasDisabledAttribute()) {
                result = groupTag.isDisabledSet();
            } else {
                EditDialogTabTag tabTag = (EditDialogTabTag) pageContext.findAttribute(DIALOG_TAB_VAR);
                if (tabTag != null && tabTag.hasDisabledAttribute()) {
                    result = tabTag.isDisabledSet();
                }
            }
        }
        return result != null ? result : getFormTag().isDisabledSet();
    }

    /**
     * returns the resource type (the path to the component) for a widget type
     */
    protected String getWidgetResourceType(String widgetType) {
        String path = context.getService(WidgetManager.class).getWidgetTypeResourcePath(context, widgetType);
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("unknown widget type '" + widgetType + "'");
        }
        return path;
    }

    /**
     * returns the property resource to edit
     */
    @Override
    public Resource getModelResource(BeanContext context) {
        Resource resource = context.getAttribute(PROPERTY_RESOURCE_ATTR, Resource.class);
        if (resource == null) {
            AbstractFormTag formTag = getFormTag();
            if (formTag instanceof EditDialogTag) {
                resource = ((EditDialogTag) formTag).getModelResource(context);
            }
            if (resource == null) {
                resource = super.getModelResource(context);
            }
        }
        return resource;
    }

    /**
     * @return the path to the owning child of the property to edit (for embedded components and their dialogs)
     */
    public String getRelativePath() {
        if (relativePath == null) {
            relativePath = context.getAttribute(PROPERTY_PATH_ATTR, String.class);
            if (StringUtils.isBlank(relativePath)) {
                String resourcePath = getResource().getPath();
                String actionPath = getFormTag().getResource().getPath();
                if (!resourcePath.equals(actionPath) && resourcePath.startsWith(actionPath)) {
                    relativePath = resourcePath.substring(actionPath.length() + 1);
                }
            }
            if (relativePath == null) {
                relativePath = "";
            } else {
                if (StringUtils.isNotBlank(relativePath) && !relativePath.endsWith("/")) {
                    relativePath += "/";
                }
            }
        }
        return relativePath;
    }

    /**
     * @return the path of the property of the resource probably with a prepended include path segment and the i18n path
     */
    public String getPropertyName() {
        if (propertyName == null) {
            propertyName = name != null ? name : getProperty();
            String relativePath = getRelativePath();
            // prepend relative path and i18n path if 'i18n' is on
            if (isI18n()) {
                AbstractFormTag formTag = getFormTag();
                if (formTag instanceof EditDialogTag) {
                    propertyName = ((EditDialogTag) formTag).getPropertyPath(relativePath, propertyName);
                } else {
                    propertyName = getI18nPath(relativePath, propertyName);
                }
            } else {
                propertyName = relativePath + propertyName;
            }
        }
        return propertyName;
    }

    @Override
    public String getRequestLanguage() {
        return request.getLocale().getLanguage();
    }

    public AbstractFormTag getFormTag() {
        return (AbstractFormTag) pageContext.findAttribute(EditDialogTag.DIALOG_VAR);
    }

    public boolean isSlingPost() {
        FormAction formAction = getFormTag().getFormAction();
        return formAction != null && SLING_POST_SERVLET_ACTION.equals(formAction.getName());
    }

    protected String getDialogActionType() {
        return getFormTag().getFormAction().getName().toLowerCase();
    }

    @Override
    public FormAction getDefaultAction() {
        return getFormTag().getDefaultAction();
    }
}
