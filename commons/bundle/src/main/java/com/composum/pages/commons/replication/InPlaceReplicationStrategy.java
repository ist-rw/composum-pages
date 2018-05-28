package com.composum.pages.commons.replication;

import com.composum.pages.commons.model.Site;
import com.composum.sling.core.JcrResource;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.platform.security.AccessMode;
import com.composum.sling.platform.staging.service.ReleaseMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the general implementation base for an in-place replication strategy
 */
public abstract class InPlaceReplicationStrategy implements ReplicationStrategy, ReleaseMapper {

    private static final Logger LOG = LoggerFactory.getLogger(InPlaceReplicationStrategy.class);

    protected ReplicationManager replicationManager;

    protected PagesReplicationConfig config;
    protected String contentPath;
    protected Pattern contentPathPattern;
    protected Pattern contentLinkPattern;

    protected PagesReplicationConfig getConfig() {
        return replicationManager.getConfig();
    }

    @Override
    public boolean releaseMappingAllowed(String path, String uri) {
        return releaseMappingAllowed(path);
    }

    @Override
    public boolean releaseMappingAllowed(String path) {
        return path.startsWith(getConfig().contentPath());
    }

    protected String getTargetPath(ReplicationContext context) {
        switch (context.accessMode) {
            case PREVIEW:
                return config.inPlacePreviewPath();
            case PUBLIC:
                return config.inPlacePublicPath();
            default:
                return null;
        }
    }

    /**
     * @return the resolver to use for traversing the released content (the replication source)
     */
    protected abstract ResourceResolver getReleaseResolver(ReplicationContext context, Resource resource);

    /**
     * the general 'canReplicate' check for all InPlace replication strategy implementations
     *
     * @return 'true' if strategy is configured and if the resource is part of a site which is replicated 'in place'
     */
    protected boolean canReplicateSite(ReplicationContext context, Resource resource) {
        boolean result = false;
        if (config != null && config.inPlaceEnabled()
                && AccessMode.AUTHOR != context.accessMode) {
            result = Site.PUBLIC_MODE_IN_PLACE.equals(context.site.getPublicMode());
        }
        return result;
    }

    /**
     * replicates a resource and its children if recursive is 'on'
     */
    @Override
    public void replicate(ReplicationContext context, Resource resource, boolean recursive)
            throws Exception {
        ResourceResolver replicateResolver = context.getResolver();
        String targetPath = getTargetPath(context);
        Resource targetRoot;
        if (StringUtils.isNotBlank(targetPath) && (targetRoot = replicateResolver.getResource(targetPath)) != null) {
            String relativePath = resource.getPath().replaceAll("^" + config.contentPath() + "/", "");
            // the 'releaseResolver' is probably a version controlled resolver
            ResourceResolver releaseResolver = getReleaseResolver(context, resource);
            Resource released = releaseResolver.getResource(resource.getPath());
            // delegation to the extension hook...
            replicate(context, targetRoot, released, relativePath, recursive, false);
        }
    }

    /**
     * extension hook to control replication in subclasses
     */
    protected void replicate(ReplicationContext context, Resource targetRoot,
                             Resource released, String relativePath, boolean recursive, boolean merge)
            throws Exception {
        Resource replicate = targetRoot.getChild(relativePath);
        if (replicate == null) {
            ResourceResolver targetResolver = context.getResolver();
            String parentPath = getParentPath(relativePath);
            Resource replicateParent = parentPath != null
                    ? buildReplicationBase(context, targetResolver, targetRoot, released.getResourceResolver(), parentPath)
                    : targetRoot;
            if (replicateParent != null) {
                replicate = createReplicate(targetResolver, released, replicateParent);
            }
        }
        if (replicate != null) {
            copyReleasedResource(context, targetRoot.getPath(), released, replicate, recursive, false);
        } else {
            LOG.error("can't create replication target for '{}', accessMode={}", released.getPath(), context.accessMode);
        }
    }

    /**
     * build the path to a resource replicate if not always existing
     * - each intermediate path is only created on demand if a resource to replicate needs this path
     */
    protected Resource buildReplicationBase(ReplicationContext context,
                                            ResourceResolver targetResolver, Resource targetRoot,
                                            ResourceResolver releaseResolver, String relativePath)
            throws Exception {
        Resource targetResource = targetRoot.getChild(relativePath);
        if (targetResource == null) {
            String contentPath = getConfig().contentPath();
            String parentPath = getParentPath(relativePath);
            Resource replicateParent = parentPath != null
                    ? buildReplicationBase(context, targetResolver, targetRoot, releaseResolver, parentPath)
                    : targetRoot;
            Resource released = releaseResolver.getResource(contentPath + "/" + relativePath);
            if (released != null) {
                targetResource = createReplicate(targetResolver, released, replicateParent);
                copyReleasedResource(context, targetRoot.getPath(), released, targetResource, false, true);
            } else {
                LOG.error("no released resource found at '{}'", contentPath + "/" + relativePath);
            }
        }
        return targetResource;
    }

    /**
     * makes a replication copy of one resource and their 'jcr:content' child if such a child exists
     * does this also with the other children if 'recursive' is 'on'
     */
    protected void copyReleasedResource(ReplicationContext context, String targetRoot, Resource released, Resource replicate,
                                        boolean recursive, boolean merge)
            throws Exception {
        copyReleasedContent(context, targetRoot, released, replicate, false, merge);
        Resource releasedContent = released.getChild(JcrConstants.JCR_CONTENT);
        if (releasedContent != null) {
            Resource replicateContent = replicate.getChild(JcrConstants.JCR_CONTENT);
            if (replicateContent == null) {
                replicateContent = createReplicate(replicate.getResourceResolver(), releasedContent, replicate);
            }
            // copy content of 'jcr:content' always recursive
            copyReleasedContent(context, targetRoot, releasedContent, replicateContent, true, merge);
        }
        if (recursive) {
            replicateChildren(context, released, true);
        }
    }

    /**
     * replication of the children is separated for reuse in subclasses
     * - for each child the replication ist delegated back to the replication manager to use the right strategy
     */
    protected void replicateChildren(ReplicationContext context, Resource released, boolean recursive)
            throws Exception {
        for (Resource releasedChild : released.getChildren()) {
            if (!JcrConstants.JCR_CONTENT.equals(releasedChild.getName())) {
                replicationManager.replicateResource(context, releasedChild, recursive);
            }
        }
    }

    /**
     * the replication of a 'jcr:content' resource is done recursive with this strategy
     */
    protected void copyReleasedContent(ReplicationContext context, String targetRoot, Resource released, Resource replicate,
                                       boolean recursive, boolean merge)
            throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("copyReleasedContent({}) to '{}'", released.getPath(), replicate.getPath());
        }
        if (!merge) {
            // in case of a 'reset' remove all children from the target resource
            ResourceResolver replicateResolver = replicate.getResourceResolver();
            for (Resource child : replicate.getChildren()) {
                replicateResolver.delete(child);
            }
        }
        copyReleasedProperties(context, targetRoot, released, replicate, merge);
        if (recursive) {
            ResourceResolver replicateResolver = replicate.getResourceResolver();
            for (Resource releasedChild : released.getChildren()) {
                String name = releasedChild.getName();
                Resource replicateChild = replicate.getChild(name);
                if (replicateChild == null) {
                    replicateChild = createReplicate(replicateResolver, releasedChild, replicate);
                }
                copyReleasedContent(context, targetRoot, releasedChild, replicateChild, true, merge);
            }
        }
    }

    protected void copyReleasedProperties(ReplicationContext context, String targetRoot,
                                          Resource released, Resource replicate, boolean merge) {
        ValueMap releasedValues = released.getValueMap();
        ModifiableValueMap replicateValues = replicate.adaptTo(ModifiableValueMap.class);
        if (!merge) {
            // in case of a 'reset' remove all properties from the target resource
            for (String key : replicateValues.keySet().toArray(new String[0])) {
                if (!ReplicationManager.REPLICATE_PROPERTY_KEEP.accept(key)) {
                    replicateValues.remove(key);
                }
            }
        }
        for (Map.Entry<String, Object> entry : releasedValues.entrySet()) {
            String key = entry.getKey();
            if (ReplicationManager.REPLICATE_PROPERTY_FILTER.accept(key)) {
                // copy content properties if not always present or a 'reset' is requested
                if (replicateValues.get(key) == null ||
                        (!merge && !ReplicationManager.REPLICATE_PROPERTY_KEEP.accept(key))) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        value = transformStringProperty(context, targetRoot, (String) value);
                    }
                    if (value != null) {
                        replicateValues.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * each string property can be a repository path
     * - if such a path can be resolved to a resource this is a reference and will be transformed
     * from the 'content' path the the replication root based path
     */
    protected String transformStringProperty(ReplicationContext context, String targetRoot, String value) {
        Matcher contentPathMatcher = contentPathPattern.matcher(value);
        if (contentPathMatcher.matches() && context.getResolver().getResource(value) != null) {
            // the the value is a reference transform the value to the replication path
            String path = contentPathMatcher.group(1);
            context.references.add(contentPath + path);
            value = targetRoot + path;
        } else {
            // if the value is not a path it can be possible that this is a rich text with embedded paths...
            value = transformTextProperty(context, targetRoot, value);
        }
        return value;
    }

    /**
     * transforms embedded references of a rich text from 'content' to the replication path
     */
    public String transformTextProperty(ReplicationContext context, String targetRoot, String value) {
        ResourceResolver resolver = context.getResolver();
        StringBuilder result = new StringBuilder();
        Matcher matcher = contentLinkPattern.matcher(value);
        int len = value.length();
        int pos = 0;
        while (matcher.find(pos)) {
            result.append(value, pos, matcher.start());
            String path = matcher.group(3);
            // check for a resolvable resource
            if (resolver.getResource(contentPath + path) != null) {
                context.references.add(contentPath + path);
                result.append(matcher.group(1));
                result.append(targetRoot);
                result.append(path);
                result.append(matcher.group(4));
            } else {
                // skip pattern unchanged if resource can't be resolved
                result.append(value, matcher.start(), matcher.end());
            }
            pos = matcher.end();
        }
        if (pos >= 0 && pos < len) {
            result.append(value, pos, len);
        }
        return result.toString();
    }

    // activation and deactivation of the strategy are controlled by the manager

    @Override
    public void activate(ReplicationManager manager) {
        this.replicationManager = manager;
        config = manager.getConfig();
        contentPath = config.contentPath();
        contentPathPattern = Pattern.compile("^" + contentPath + "(/.*)$");
        contentLinkPattern = Pattern.compile("<(a\\s+(.+\\s+)?href=['\"])" + contentPath + "(/[^'\"]+)(['\"])");
    }

    @Override
    public void deactivate(ReplicationManager manager) {
        this.replicationManager = null;
        this.config = null;
    }

    // helpers

    /**
     * creates a new replicate resource with all types of the origin but without the properties and children
     */
    protected Resource createReplicate(ResourceResolver replicateResolver, Resource released, Resource parent)
            throws PersistenceException {
        Map<String, Object> properties = new HashMap<>();
        // determine the primary type of the origin
        String primaryType = released instanceof JcrResource
                ? ((JcrResource) released).getPrimaryType()
                : ResourceHandle.use(released).getPrimaryType();
        properties.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
        // determine the mixin types of the replicate
        String[] mixinTypes = released.getValueMap().get(JcrConstants.JCR_MIXINTYPES, String[].class);
        if (mixinTypes != null) {
            List<String> acceptedMixins = new ArrayList<>();
            for (String mixinType : mixinTypes) {
                if (ReplicationManager.REPLICATE_MIXIN_TYPES_FILTER.accept(mixinType)) {
                    acceptedMixins.add(mixinType);
                }
            }
            if (acceptedMixins.size() > 0) {
                properties.put(JcrConstants.JCR_MIXINTYPES, acceptedMixins.toArray(new String[0]));
            }
        }
        // create the new resource with the same name as the origin
        return replicateResolver.create(parent, released.getName(), properties);
    }

    public static String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : null;
    }
}
