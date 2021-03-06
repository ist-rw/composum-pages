/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.pages.commons.service;

import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.model.properties.PathPatternSet;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface ResourceManager {

    /**
     * the reference to a potentially non existing (static included; can exist but mustn't) content resource
     * this is a simple transferable (JSON) resource description without the overhead of a NonExistingResource and
     * with access to the resources properties (if resource exists), the configuration and the resource type properties
     */
    interface ResourceReference {

        @Nonnull
        String getPath();

        @Nonnull
        String getType();

        boolean isExisting();

        @Nonnull
        Resource getResource();

        @Nonnull
        String getPrimaryType();

        @Nonnull
        JsonObject getEditData();

        @Nonnull
        ResourceResolver getResolver();

        /**
         * returns the property value using the cascade: resource - configuration - resource type
         */
        @Nonnull
        <T extends Serializable> T getProperty(@Nonnull String name, @Nonnull T defaultValue);

        /**
         * returns the property value using the cascade: resource - configuration - resource type
         */
        @Nullable
        <T extends Serializable> T getProperty(@Nonnull String name, @Nonnull Class<T> type);

        /**
         * returns the property value using the cascade: resource - resource type - design;
         */
        <T extends Serializable> T getRuleProperty(@Nonnull String name, @Nonnull Class<T> type);
    }

    /**
     * a list of references (simple transferable as a JSON array)
     */
    interface ReferenceList extends List<ResourceReference> {

        void fromJson(ResourceResolver resolver, JsonReader reader) throws IOException;

        void toJson(JsonWriter writer) throws IOException;
    }

    /**
     * a page template to create pages as copy of the template and to reference design rules embedded in the template
     */
    interface Template extends Serializable {

        String getPath();

        String getResourceType();

        @Nonnull
        Resource getTemplateResource(ResourceResolver resolver);

        @Nonnull
        PathPatternSet getTypePatterns(@Nonnull ResourceResolver resolver, @Nonnull String propertyName);

        // design configuration

        /**
         * Retrieves es the design rules for an element of a page; the design rules are configured as elements
         * of a 'cpp:design' child node of the templates content resource.
         *
         * @param pageContent  the content resource of the page
         * @param relativePath the path of the element relative to the pages content resource
         * @param resourceType the designated or overlayed resource type of the content element
         * @return the design model; 'null' if no design rules found
         */
        @Nullable
        Design getDesign(@Nonnull Resource pageContent,
                         @Nonnull String relativePath, @Nullable String resourceType);
    }

    /**
     * a design rule for a content element determined from the template of the elements page
     */
    interface Design extends Serializable {

        @Nonnull
        String getPath();

        @Nonnull
        Resource getResource(@Nonnull ResourceResolver resolver);

        @Nullable
        <T extends Serializable> T getProperty(@Nonnull ResourceResolver resolver,
                                               @Nonnull String name, @Nonnull Class<T> type);
    }

    // references can be built from various sources...

    ResourceReference getReference(AbstractModel model);

    /**
     * a resource and a probably overlayed type (type can be 'null')
     */
    ResourceReference getReference(@Nonnull Resource resource, @Nullable String type);

    ResourceReference getReference(@Nonnull ResourceResolver resolver, @Nonnull String path, @Nullable String type);

    /**
     * a reference translated from a JSON object (transferred reference)
     */
    ResourceReference getReference(ResourceResolver resolver, JsonReader reader) throws IOException;

    ReferenceList getReferenceList();

    ReferenceList getReferenceList(ResourceManager.ResourceReference... references);

    ReferenceList getReferenceList(ResourceResolver resolver, String jsonValue) throws IOException;

    ReferenceList getReferenceList(ResourceResolver resolver, JsonReader reader) throws IOException;

    //
    // page templates and hierarchy rules
    //

    @Nonnull
    Template toTemplate(@Nonnull Resource resource);

    @Nullable
    Template getTemplateOf(@Nullable Resource resource);

    @Nullable
    Resource findContainingPageResource(@Nullable Resource resource);

    /**
     * Checks the policies of the resource hierarchy for a given parent and child (for move and copy operations).
     *
     * @param resolver the resolver of the current request
     * @param parent   the designated parent resource
     * @param child    the resource to check
     * @return 'true' if the child could be a child of the given parent
     */
    boolean isAllowedChild(@Nonnull ResourceResolver resolver, @Nonnull Resource parent, @Nonnull Resource child);

    //
    // content move operation
    //

    /**
     * Moves a resource and adopts all references to the moved resource or one of its children.
     *
     * @param resolver         the resolver (session context)
     * @param changeRoot       the root element for reference search and change
     * @param source           the resource to move
     * @param targetParent     the target (the parent resource) of the move
     * @param newName          an optional new name for the resource
     * @param before           the designated sibling in an ordered target collection
     * @param updatedReferrers output parameter: the List of referers found - these were changed and might need setting a last modification date
     * @return the new resource at the target path
     */
    @Nonnull
    Resource moveContentResource(@Nonnull ResourceResolver resolver, @Nonnull Resource changeRoot,
                                 @Nonnull Resource source, @Nonnull Resource targetParent,
                                 @Nullable String newName, @Nullable Resource before,
                                 @Nonnull List<Resource> updatedReferrers)
            throws RepositoryException, PersistenceException;

    /**
     * Changes the 'oldPath' references in each property of a tree to the 'newPath'.
     * Caution: this does *not* update the last modification date of referrers - that has to be done later from {foundReferrers}.
     *
     * @param resourceFilter change all resources accepted by this filter, let all other resources unchanged
     * @param propertyFilter change only the properties with names matching to this property name filter
     * @param resource       the resource to change (recursive! - the root in the initial call)
     * @param foundReferrers the List of referers found (to fill during traversal, empty in the initial call)
     * @param scanOnly       if 'true' no changes are made but the referer list is filled
     * @param oldPath        the old path of a moved resource
     * @param newPath        the new path of the resource
     */
    void changeReferences(@Nonnull ResourceFilter resourceFilter, @Nonnull StringFilter propertyFilter,
                          @Nonnull Resource resource, @Nonnull List<Resource> foundReferrers, boolean scanOnly,
                          @Nonnull String oldPath, @Nonnull String newPath)
            throws PersistenceException;

    /**
     * Changes the 'oldTypePattern' resource types in every appropriate component using the 'newTypeRule'.
     *
     * @param resourceFilter change all resources accepted by this filter, let all other resources unchanged
     * @param resource       the resource to change (recursive! - the root in the initial call)
     * @param oldTypePattern the resource type pattern to change
     * @param newTypeRule    the pattern matcher rule to build the new type
     */
    void changeResourceType(@Nonnull ResourceFilter resourceFilter, @Nonnull Resource resource,
                            @Nonnull String oldTypePattern, @Nonnull String newTypeRule)
            throws PersistenceException;

    //
    // templates and copies
    //

    interface TemplateContext {

        ResourceResolver getResolver();

        String applyTemplatePlaceholders(@Nonnull Resource target, @Nonnull String value);
    }

    /**
     * the 'transform nothing' context used in case of a copy operation
     */
    class NopTemplateContext implements TemplateContext {

        protected final ResourceResolver resolver;

        public NopTemplateContext(ResourceResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public ResourceResolver getResolver() {
            return resolver;
        }

        @Override
        public String applyTemplatePlaceholders(@Nonnull Resource target, @Nonnull String value) {
            return value;
        }
    }

    /**
     * only properties of a template accepted by this filter are copied (filter out template settings)
     */
    StringFilter CONTENT_PROPERTY_FILTER = new StringFilter.BlackList(
            "^jcr:(primaryType|created.*|uuid)$",
            "^jcr:(baseVersion|predecessors|versionHistory|isCheckedOut)$",
            "^(allowed|forbidden)(Child|Parent)(Elements|Containers|Templates|Types)$",
            "^(allowed|forbidden)(Paths)$",
            "^isTemplate$"
    );

    /**
     * properties of a target accepted by this filter are not replaced by values from a template
     */
    StringFilter CONTENT_TARGET_KEEP = new StringFilter.WhiteList(
            "^jcr:(primaryType|created.*|uuid)$",
            "^jcr:(baseVersion|predecessors|versionHistory|isCheckedOut)$"
    );

    /**
     * only properties of a template accepted by this filter are copied (filter out template settings)
     */
    StringFilter TEMPLATE_PROPERTY_FILTER = new StringFilter.BlackList(
            "^jcr:(primaryType|created.*|uuid)$",
            "^jcr:(baseVersion|predecessors|versionHistory|isCheckedOut)$",
            "^(allowed|forbidden)(Child|Parent)(Elements|Containers|Templates|Types)$",
            "^(allowed|forbidden)(Paths)$",
            "^isTemplate$",
            "^siteComponentSettings",
            "^jcr:(title|description)$"
    );

    /**
     * properties of a target accepted by this filter are not replaced by values from a template
     */
    StringFilter TEMPLATE_TARGET_KEEP = new StringFilter.WhiteList(
            "^jcr:(primaryType|created.*|uuid)$",
            "^jcr:(baseVersion|predecessors|versionHistory|isCheckedOut)$",
            "^jcr:(title|description)$"
    );

    /**
     * Creates a new resource as a copy of another resource.
     *
     * @param resolver the resolver to use for CRUD operations
     * @param template the template content resource
     * @param parent   the parent resource for the new resource
     * @param name     the name of the new resource
     * @param before   the designated sibling in an ordered target collection
     * @return the resource created
     * @throws PersistenceException if an error is occurring
     */
    Resource copyContentResource(@Nonnull ResourceResolver resolver, @Nonnull Resource template,
                                 @Nonnull Resource parent, @Nonnull String name, @Nullable Resource before)
            throws PersistenceException;

    /**
     * @return 'true' if the resource is a content template
     */
    boolean isTemplate(@Nonnull BeanContext context, @Nonnull Resource resource);

    /**
     * Creates a new resource as a copy of a template. If content nodes of such a template are referencing
     * other templates by a 'template' property the content of these referenced templates is copied
     * (used in site templates to reference the normal page templates of the site inside of a site template).
     *
     * @param context             the resolver to use for CRUD operations
     * @param parent              the parent resource for the new resource
     * @param name                the name of the new resource
     * @param template            the template content resource
     * @param setTemplateProperty if 'true' the 'template' property is filled with the template path
     * @return the resource created
     * @throws PersistenceException if an error is occurring
     */
    Resource createFromTemplate(@Nonnull TemplateContext context, @Nonnull Resource parent, @Nonnull String name,
                                @Nonnull Resource template, boolean setTemplateProperty)
            throws PersistenceException, RepositoryException;
}
