package com.composum.pages.commons.taglib;

import com.composum.sling.core.BeanContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Locale;

import static com.composum.platform.models.annotations.InternationalizationStrategy.I18NFOLDER;

/**
 * a tag to instantiate a model object for a property of a component (property node set)
 */
public class PropertyTag extends ModelTag {

    protected String property;
    protected boolean i18n = false;

    @Override
    protected void clear() {
        i18n = false;
        property = null;
        super.clear();
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

    /**
     * returns the property resource
     */
    @Override
    public Resource getModelResource(BeanContext context) {
        ResourceResolver resolver = context.getResolver();
        Resource resource = super.getModelResource(context);
        Resource propertyResource = null;
        if (isI18n()) {
            Locale locale = context.getRequest().adaptTo(Locale.class);
            List<String> i18nPaths = I18NFOLDER.getI18nPaths(locale);
            for (String path : i18nPaths) {
                if ((propertyResource = resolver.getResource(resource, path + '/' + property)) != null) {
                    break;
                }
            }
        }
        if (propertyResource == null) {
            propertyResource = resolver.resolve(resource.getPath() + "/" + property);
        }
        return propertyResource;
    }
}
