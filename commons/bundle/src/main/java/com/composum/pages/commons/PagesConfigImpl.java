package com.composum.pages.commons;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.mapping.jcr.ResourceFilterMapping;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static com.composum.sling.core.CoreConfiguration.TREE_INTERMEDIATE_FILTER_KEY;

/**
 * The configuration service for all servlets in the pages bundle.
 */
@Component(
        label = "Composum Pages Configuration",
        description = "the configuration service for all servlets in the pages bundles",
        immediate = true,
        metatype = true
)
@Service
public class PagesConfigImpl implements PagesConfiguration {

    @Property(
            name = SITE_NODE_FILTER_KEY,
            label = "Sites Filter",
            description = "the filter configuration to set the scope to the  internet sites",
            value = "PrimaryType(+'^cpp:(Element|Container|Site)$')"
    )
    private ResourceFilter siteNodeFilter;

    @Property(
            name = PAGE_NODE_FILTER_KEY,
            label = "Content Page Filter",
            description = "the filter configuration to set the scope to the content pages",
            value = "PrimaryType(+'^cpp:(Page|Site)$')"
    )
    private ResourceFilter pageNodeFilter;

    @Property(
            name = CONTAINER_NODE_FILTER_KEY,
            label = "Container resource filter",
            description = "the filter configuration to set the scope to the content containers",
            value = "PrimaryType(+'^cpp:(Container|Page|Site)$')"
    )
    private ResourceFilter containerNodeFilter;

    @Property(
            name = COMPONENT_NODE_FILTER_KEY,
            label = "Component resource filter",
            description = "the filter configuration to set the scope to the content components",
            value = "PrimaryType(+'^cpp:(Element|Container|Page|Site)$')"
    )
    private ResourceFilter componentNodeFilter;

    @Property(
            name = DEVELOPMENT_TREE_FILTER_KEY,
            label = "Development tree filter",
            description = "the filter configuration to set the scope to component development",
            value = "PrimaryType(+'^cpp:(Site|Component|Theme)$')"
    )
    private ResourceFilter develomentTreeFilter;

    @Property(
            name = COMPONENT_INTERMEDIATE_FILTER_KEY,
            label = "Page Content Intermediate Filter",
            description = "the filter configuration to determine all intermediate nodes in the content structure",
            value = "PrimaryType(+'^cpp:(PageContent)$')"
    )
    private ResourceFilter componentIntermediateFilter;

    @Property(
            name = SITE_INTERMEDIATE_FILTER_KEY,
            label = "Site Configuration Intermediate Filter",
            description = "the filter configuration to determine all intermediate nodes in the site definition",
            value = "or{PrimaryType(+'^cpp:(SiteConfiguration)$'),ResourceType(+'^composum/pages/stage/edit/template(/(content|set))?$')}"
    )
    private ResourceFilter siteIntermediateFilter;

    @Property(
            name = DEV_INTERMEDIATE_FILTER_KEY,
            label = "Development Intermediate Filter",
            description = "the filter configuration to determine all intermediate nodes in the development scope",
            value = "and{Folder(),Path(+'^/(etc|conf|apps|libs|sightly|htl|var)')}"
    )
    private ResourceFilter devIntermediateFilter;

    @Property(
            name = TREE_INTERMEDIATE_FILTER_KEY,
            label = "Tree Intermediate (Folder) Filter",
            description = "the filter configuration to determine all intermediate nodes in the tree view",
            value = "and{Folder(),Path(-'^/(etc|conf|apps|libs|sightly|htl|var)')}"
    )
    private ResourceFilter treeIntermediateFilter;

    @Property(
            name = ORDERABLE_NODES_FILTER_KEY,
            label = "Orderable Nodes Filter",
            description = "the filter configuration to detect ordered nodes (prevent from sorting in the tree)",
            value = "or{Type(+[node:orderable]),PrimaryType(+'^.*([Oo]rdered|[Pp]age).*$')}"
    )
    private ResourceFilter orderableNodesFilter;

    private static final ResourceFilter REPLICATION_ROOT_FILTER =
            new ResourceFilter.PathFilter(new StringFilter.BlackList("^/(public|preview)"));

    private Map<String, Boolean> enabledServlets;

    @Override
    public boolean isEnabled(AbstractServiceServlet servlet) {
        Boolean result = enabledServlets.get(servlet.getClass().getSimpleName());
        return result != null ? result : false;
    }

    @Override
    public ResourceFilter getSiteNodeFilter() {
        return siteNodeFilter;
    }

    @Override
    public ResourceFilter getPageNodeFilter() {
        return pageNodeFilter;
    }

    @Override
    public ResourceFilter getContainerNodeFilter() {
        return containerNodeFilter;
    }

    @Override
    public ResourceFilter getComponentNodeFilter() {
        return componentNodeFilter;
    }

    @Override
    public ResourceFilter getDevelopmentTreeFilter() {
        return develomentTreeFilter;
    }

    @Override
    public ResourceFilter getTreeIntermediateFilter() {
        return treeIntermediateFilter;
    }

    @Override
    public ResourceFilter getOrderableNodesFilter() {
        return orderableNodesFilter;
    }

    public Dictionary getProperties() {
        return properties;
    }

    protected Dictionary properties;

    /**
     * Creates a 'tree filter' as combination with the configured filter and the rules for the
     * 'intermediate' nodes (folders) to traverse up to the target nodes.
     *
     * @param configuredFilter the filter for the target nodes
     */
    protected ResourceFilter buildTreeFilter(ResourceFilter configuredFilter,
                                             ResourceFilter intermediateFilter) {
        return new ResourceFilter.FilterSet(
                ResourceFilter.FilterSet.Rule.tree, configuredFilter, intermediateFilter);
    }

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.properties = context.getProperties();
        orderableNodesFilter = ResourceFilterMapping.fromString(
                (String) properties.get(ORDERABLE_NODES_FILTER_KEY));
        treeIntermediateFilter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                REPLICATION_ROOT_FILTER,
                ResourceFilterMapping.fromString((String) properties.get(TREE_INTERMEDIATE_FILTER_KEY)));
        siteIntermediateFilter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                REPLICATION_ROOT_FILTER,
                new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.or,
                        ResourceFilterMapping.fromString((String) properties.get(SITE_INTERMEDIATE_FILTER_KEY)),
                        treeIntermediateFilter));
        siteNodeFilter = buildTreeFilter(new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                        REPLICATION_ROOT_FILTER,
                        ResourceFilterMapping.fromString((String) properties.get(SITE_NODE_FILTER_KEY))),
                siteIntermediateFilter);
        pageNodeFilter = buildTreeFilter(new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                        REPLICATION_ROOT_FILTER,
                        ResourceFilterMapping.fromString((String) properties.get(PAGE_NODE_FILTER_KEY))),
                treeIntermediateFilter);
        componentIntermediateFilter = new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.or,
                ResourceFilterMapping.fromString((String) properties.get(COMPONENT_INTERMEDIATE_FILTER_KEY)),
                treeIntermediateFilter);
        containerNodeFilter = buildTreeFilter(new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                        REPLICATION_ROOT_FILTER,
                        ResourceFilterMapping.fromString((String) properties.get(CONTAINER_NODE_FILTER_KEY))),
                componentIntermediateFilter);
        componentNodeFilter = buildTreeFilter(new ResourceFilter.FilterSet(ResourceFilter.FilterSet.Rule.and,
                        REPLICATION_ROOT_FILTER,
                        ResourceFilterMapping.fromString((String) properties.get(COMPONENT_NODE_FILTER_KEY))),
                componentIntermediateFilter);
        devIntermediateFilter = ResourceFilterMapping.fromString(
                (String) properties.get(DEV_INTERMEDIATE_FILTER_KEY));
        develomentTreeFilter = buildTreeFilter(ResourceFilterMapping.fromString(
                (String) properties.get(DEVELOPMENT_TREE_FILTER_KEY)), devIntermediateFilter);
        enabledServlets = new HashMap<>();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.properties = null;
    }
}
