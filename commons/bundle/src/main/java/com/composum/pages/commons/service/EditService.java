package com.composum.pages.commons.service;

import com.composum.pages.commons.model.ElementTypeFilter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.util.List;

/**
 * the service interface for Pages editing operations
 */
public interface EditService {

    /**
     * @return 'true' if the element can be a child of the container
     */
    boolean isAllowedElement(@Nonnull ResourceResolver resolver,
                             @Nonnull ResourceManager.ResourceReference container,
                             @Nonnull ResourceManager.ResourceReference element);

    /**
     * Determines the list of potential target containers for a page content element.
     */
    ResourceManager.ReferenceList filterTargetContainers(ResourceResolver resolver,
                                                         ResourceManager.ReferenceList candidates,
                                                         ResourceManager.ResourceReference element);

    /**
     * Determines the list of resource types (nodes of type 'cpp:Component') for any of the containers.
     *
     * @param resolver   the requests resolver (session)
     * @param containers the set of designated container references
     * @return the result of a component type query filtered by the filter object
     */
    List<String> getAllowedElementTypes(ResourceResolver resolver,
                                        ResourceManager.ReferenceList containers,
                                        boolean resourceTypePath);

    /**
     * Determines the list of resource types (nodes of type 'cpp:Component') which are accepted by the filter.
     *
     * @param resolver   the requests resolver (session)
     * @param containers the set of designated container references
     * @param filter     the filter instance (resource type pattern filter)
     * @return the result of a component type query filtered by the filter object
     */
    List<String> getAllowedElementTypes(ResourceResolver resolver,
                                        ResourceManager.ReferenceList containers,
                                        ElementTypeFilter filter,
                                        boolean resourceTypePath);

    /**
     * Returns or creates and returns the resource addressed by a reference.
     *
     * @param resolver  the resolver to use for resource resolving and creation
     * @param reference the path and type of the resource
     * @return the resource instance
     */
    Resource getReferencedResource(ResourceResolver resolver, ResourceManager.ResourceReference reference)
            throws PersistenceException;

    /**
     * Inserts a new resource.
     *
     * @param resolver     the resolver (session context)
     * @param resourceType the type of the new resource
     * @param target       the target (the parent resource) of the move
     * @param before       the designated sibling in an ordered target collection
     */
    void insertComponent(ResourceResolver resolver, String resourceType,
                         ResourceManager.ResourceReference target, Resource before)
            throws RepositoryException, PersistenceException;

    /**
     * Moves a resource and adopts all references to the moved resource or one of its children.
     *
     * @param resolver     the resolver (session context)
     * @param changeRoot   the root element for reference search and change
     * @param source       the resource to move
     * @param targetParent the target (a reference to the parent resource) of the move
     * @param before       the designated sibling in an ordered target collection
     * @return the new resource at the target path
     */
    Resource moveComponent(ResourceResolver resolver, Resource changeRoot,
                           Resource source, ResourceManager.ResourceReference targetParent, Resource before)
            throws RepositoryException, PersistenceException;
}
