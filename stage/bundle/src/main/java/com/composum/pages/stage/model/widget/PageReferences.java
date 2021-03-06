package com.composum.pages.stage.model.widget;

import com.composum.pages.commons.PagesConstants.ReferenceType;
import com.composum.pages.commons.model.Page;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * the PageReferrers model...
 */
public class PageReferences extends ReferencesWidget {

    public static final String ATTR_UNRESOLVED = "unresolved";

    private transient ReferenceType scope;
    private transient Boolean unresolved;

    @Override
    protected List<Reference> retrieveCandidates(@Nonnull final Page referrer) {
        List<Reference> references = new ArrayList<>();
        Collection<Resource> resources = getPageManager().getReferences(referrer, getScope(), isUnresolved(), true);
        for (Resource resource : resources) {
            references.add(new Reference(referrer, resource));
        }
        return references;
    }

    public ReferenceType getScope() {
        return scope;
    }

    public boolean isUnresolved() {
        return unresolved != null ? unresolved : false;
    }

    @Override
    public String filterWidgetAttribute(@Nonnull String attributeKey, Object attributeValue) {
        if (ATTR_SCOPE.equals(attributeKey)) {
            scope = attributeValue instanceof ReferenceType ? (ReferenceType) attributeValue
                    : attributeValue != null ? ReferenceType.valueOf(attributeValue.toString()) : null;
            return null;
        } else if (ATTR_UNRESOLVED.equals(attributeKey)) {
            unresolved = attributeValue instanceof Boolean ? (Boolean) attributeValue
                    : attributeValue != null ? Boolean.valueOf(attributeValue.toString()) : null;
            return null;
        }
        return super.filterWidgetAttribute(attributeKey, attributeValue);
    }
}
