package com.composum.pages.commons.model;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.util.ResolverUtil;
import com.composum.pages.commons.util.ResourceTypeUtil;
import com.composum.platform.models.annotations.DetermineResourceStategy;
import com.composum.platform.models.annotations.PropertyDetermineResourceStrategy;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.pages.commons.PagesConstants.NODE_TYPE_PAGE;
import static com.composum.pages.commons.PagesConstants.NT_COMPONENT;
import static com.composum.pages.commons.PagesConstants.PN_COMPONENT_TYPE;
import static com.composum.pages.commons.servlet.EditServlet.EDIT_RESOURCE_TYPE_KEY;
import static com.composum.pages.commons.util.ResourceTypeUtil.isSyntheticResource;

/**
 * the delegate class for a component itself (the implementation)
 */
@PropertyDetermineResourceStrategy(Component.TypeResourceStrategy.class)
public class Component extends AbstractModel {

    public static final String EDIT_PATH = "edit/";

    public static final String EDIT_DIALOG_PATH = EDIT_PATH + "dialog";
    public static final String CREATE_DIALOG_PATH = EDIT_DIALOG_PATH + "/create";
    public static final String DELETE_DIALOG_PATH = EDIT_DIALOG_PATH + "/delete";

    public static final String EDIT_TILE_PATH = EDIT_PATH + "tile";
    public static final String THUMBNAIL_PATH = EDIT_PATH + "thumbnail";
    public static final String HELP_PAGE_PATH = EDIT_PATH + "help";

    public static final String EDIT_TOOLBAR_PATH = EDIT_PATH + "toolbar";
    public static final String TREE_ACTIONS_PATH = EDIT_PATH + "tree";
    public static final String CONTEXT_ACTIONS_PATH = EDIT_PATH + "context/actions";

    public static final String TYPE_HINT_PARAM = "type";

    public static final Pattern PRIMARY_TYPE_PATTERN = Pattern.compile("^[^:/]+:.+");
    public static final Pattern EDIT_SUBTYPE_PATTERN = Pattern.compile(
            "^(.+)/edit(/(default|actions)/[^/]+)?/(dialog(/.+)?|toolbar|tree|tile|context/.+)$"
    );

    /**
     * check the 'cpp:Component' type for a resource
     */
    public static boolean isComponent(Resource resource) {
        return ResourceUtil.isResourceType(resource, NT_COMPONENT);
    }

    // declared component properties

    public class Property extends AbstractModel {

        public final String propertyPath;

        public Property(@Nonnull final String propertyPath, @Nonnull final Resource resource) {
            this.propertyPath = propertyPath;
            initialize(Component.this.getContext(), resource);
        }

        @Nonnull
        public String getPropertyPath() {
            return propertyPath;
        }

        @Nonnull
        public String getPropertyType() {
            return getProperty("type", "String");
        }

        public boolean isRequired() {
            return getProperty("required", Boolean.FALSE);
        }

        public boolean isMulti() {
            return getProperty("multi", Boolean.FALSE);
        }

        public boolean isI18n() {
            return getProperty("i18n", Boolean.FALSE);
        }

        @Nullable
        public String getTextType() {
            return "String".equals(getPropertyType()) ? getProperty("text", "") : null;
        }
    }

    public class Properties extends LinkedHashMap<String, Property> {

        public Properties(@Nonnull final ResourceFilter filter) {
            scanProperties("", getPropertiesDeclaration(), filter);
        }

        protected void scanProperties(@Nonnull final String path, @Nullable final Resource node,
                                      @Nonnull final ResourceFilter filter) {
            if (node != null) {
                for (Resource child : node.getChildren()) {
                    String childPath = path + child.getName();
                    if (filter.accept(child)) {
                        Property property = new Property(childPath, child);
                        put(childPath, property);
                    }
                    scanProperties(childPath + "/", child, filter);
                }
            }
        }
    }

    // declared content elements

    public class Element extends AbstractModel {

        public final String elementPath;

        public Element(@Nonnull final String elementPath, @Nonnull final Resource resource) {
            this.elementPath = elementPath;
            initialize(Component.this.getContext(), resource);
        }

        @Nonnull
        public String getElementPath() {
            return elementPath;
        }

        @Nullable
        public String getElementType() {
            return getProperty("type", String.class);
        }
    }

    public class Elements extends LinkedHashMap<String, Element> {

        public Elements(@Nonnull final ResourceFilter filter) {
            scanElements("", getElementsDeclaration(), filter);
        }

        /**
         * scan elements declaration
         */
        protected void scanElements(@Nonnull final String path, @Nullable final Resource node,
                                    @Nonnull final ResourceFilter filter) {
            if (node != null) {
                for (Resource child : node.getChildren()) {
                    String childPath = path + child.getName();
                    if (filter.accept(child)) {
                        Element element = new Element(childPath, child);
                        put(childPath, element);
                    }
                    scanElements(childPath + "/", child, filter);
                }
            }
        }

        /**
         * retrieve corresponding set of elements of a component instance
         */
        @Nullable
        public Map<String, Model> instanceElements(@Nonnull final Model model) {
            return instanceElements(model.getContext(), model.getResource());
        }

        /**
         * retrieve corresponding set of elements of a component instance
         */
        @Nullable
        public Map<String, Model> instanceElements(@Nonnull final BeanContext context,
                                                   @Nonnull final Resource resource) {
            Map<String, Model> elements = null;
            if (!isEmpty()) {
                elements = new LinkedHashMap<>();
                for (Map.Entry<String, Component.Element> entry : entrySet()) {
                    String relativePath = entry.getValue().elementPath;
                    Resource child = resource.getChild(relativePath);
                    if (child != null) {
                        GenericModel element = new GenericModel(context, child);
                        elements.put(relativePath, element);
                    }
                }
            }
            return elements;
        }
    }

    // component implementation elements

    public static class ComponentPieces {

        public final boolean editDialog;
        public final boolean createDialog;
        public final boolean deleteDialog;

        public final boolean editTile;
        public final boolean thumbnail;
        public final boolean helpPage;

        public final boolean editToolbar;
        public final boolean treeActions;
        public final boolean contextActions;

        public ComponentPieces(Resource component) {
            this(component.getChild(EDIT_DIALOG_PATH) != null,
                    component.getChild(CREATE_DIALOG_PATH) != null,
                    component.getChild(DELETE_DIALOG_PATH) != null,
                    component.getChild(EDIT_TILE_PATH) != null,
                    component.getChild(THUMBNAIL_PATH) != null,
                    component.getChild(HELP_PAGE_PATH) != null,
                    component.getChild(EDIT_TOOLBAR_PATH) != null,
                    component.getChild(TREE_ACTIONS_PATH) != null,
                    component.getChild(CONTEXT_ACTIONS_PATH) != null);
        }

        public ComponentPieces(boolean editDialog, boolean createDialog, boolean deleteDialog,
                               boolean editTile, boolean thumbnail, boolean helpPage,
                               boolean editToolbar, boolean treeActions, boolean contextActions) {
            this.editDialog = editDialog;
            this.createDialog = createDialog;
            this.deleteDialog = deleteDialog;
            this.editTile = editTile;
            this.thumbnail = thumbnail;
            this.helpPage = helpPage;
            this.editToolbar = editToolbar;
            this.treeActions = treeActions;
            this.contextActions = contextActions;
        }

        public boolean isEditDialog() {
            return editDialog;
        }

        public boolean isCreateDialog() {
            return createDialog;
        }

        public boolean isDeleteDialog() {
            return deleteDialog;
        }

        public boolean isEditTile() {
            return editTile;
        }

        public boolean isThumbnail() {
            return thumbnail;
        }

        public boolean isHelpPage() {
            return helpPage;
        }

        public boolean isEditToolbar() {
            return editToolbar;
        }

        public boolean isTreeActions() {
            return treeActions;
        }

        public boolean isContextActions() {
            return contextActions;
        }
    }

    enum TumbnailType {component, imageRef, imageFile}

    public class Thumbnail {

        protected TumbnailType type = TumbnailType.component;

        protected Resource resource;

        protected String imageRef;

        public Thumbnail() {
            resource = Component.this.getResource().getChild(THUMBNAIL_PATH);
            if (resource != null) {
                ValueMap values = resource.getValueMap();
                imageRef = values.get(Image.PROP_IMAGE_REF, "");
                if (StringUtils.isNotBlank(imageRef)) {
                    type = TumbnailType.imageRef;
                } else {
                    List<Resource> files = ResourceUtil.getChildrenByType(resource, JcrConstants.NT_FILE);
                    if (files.size() > 0) {
                        type = TumbnailType.imageFile;
                        imageRef = files.get(0).getPath();
                    }
                }
            }
        }

        public TumbnailType getType() {
            return type;
        }

        public String getImageRef() {
            return imageRef;
        }
    }

    /**
     * the delegate of the components dialog implemented as a 'subcomponent'
     */
    public class EditDialog extends AbstractModel {

        private transient Resource thumbnailImage;

        public EditDialog() {
            super();
            Resource subtypeResource = ResourceTypeUtil.getSubtype(Component.this.resolver,
                    null, Component.this.getPath(), ResourceTypeUtil.EDIT_DIALOG_PATH, null);
            // a component mustn't have a dialog implementation...
            initialize(Component.this.context, subtypeResource != null
                    ? subtypeResource
                    : new NonExistingResource(Component.this.resolver,
                    Component.this.getPath() + "/" + ResourceTypeUtil.EDIT_DIALOG_PATH));
        }

        /**
         * returns false if no dialog is configured
         */
        public boolean isValid() {
            return !ResourceUtil.isNonExistingResource(resource);
        }

        /**
         * returns true if the dialog of a resource supertype is used
         */
        public boolean isInherited() {
            String path = getPath();
            return StringUtils.isNotBlank(path) && !path.startsWith(Component.this.getPath());
        }

        public boolean getHasThumbnailImage() {
            return getThumbnailImage() != null;
        }

        public Resource getThumbnailImage() {
            if (thumbnailImage == null) {
                thumbnailImage = resource.getChild("thumbnail.png");
                if (thumbnailImage == null) {
                    thumbnailImage = resource.getChild("thumbnail.jpg");
                }
            }
            return thumbnailImage;
        }
    }

    /**
     * the delegate of the components tile implemented as a 'subcomponent'
     */
    public class EditTile extends AbstractModel {

        public EditTile() {
            super();
            Resource subtypeResource = ResourceTypeUtil.getSubtype(Component.this.resolver,
                    null, Component.this.getPath(), ResourceTypeUtil.EDIT_TILE_PATH, null);
            initialize(Component.this.context, subtypeResource);
        }
    }

    /**
     * transient (lazy loaded) attributes
     */

    private transient EditDialog editDialog;
    private transient EditTile editTile;

    private transient String type;

    private transient Thumbnail thumbnail;
    private transient String helpContent;

    private transient ComponentPieces componentPieces;

    private transient Map<String, Properties> properties = new TreeMap<>();
    private transient Resource propertiesDeclaration;
    private transient Map<String, Elements> elements = new LinkedHashMap<>();
    private transient Resource elementsDeclaration;

    /**
     * delegate initialization
     */

    public Component() {
    }

    public Component(BeanContext context, Resource resource) {
        initialize(context, resource);
    }

    /**
     * determine the components resource even if the initial resource is an instance of the component
     */
    @Override
    @Nullable
    protected Resource determineResource(@Nullable Resource initialResource) {
        Resource typeResource = null;
        if (Component.isComponent(initialResource)) {
            typeResource = initialResource;
        } else if (initialResource != null) {
            typeResource = getTypeResource(initialResource);
        }
        return typeResource != null ? typeResource : initialResource;
    }

    /**
     * determines the resource of the component (of the 'implementation') even
     * if the resource is an instance of the component (content resource)
     */
    protected Resource getTypeResource(Resource resource) {
        return TypeResourceStrategy.getTypeResource(resource, resolver, context);
    }

    /**
     * Compatible to {@link AbstractModel#determineResource(Resource)}.
     */
    public static class TypeResourceStrategy implements DetermineResourceStategy {

        /**
         * Compatible to {@link AbstractModel#determineResource(Resource)}.
         */
        @Override
        public Resource determineResource(BeanContext beanContext, Resource requestResource) {
            // ignore all resource types modified by resource wrappers
            Resource typeResource = getTypeResource(requestResource, beanContext.getResolver(), beanContext);
            return typeResource != null ? typeResource : requestResource;
        }

        /**
         * determines the resource of the component (of the 'implementation') even
         * if the resource is an instance of the component (content resource)
         */
        public static Resource getTypeResource(Resource resource, ResourceResolver resolver, BeanContext context) {
            Resource typeResource = null;
            if (!isSyntheticResource(resource)) {
                // ignore all resource types modified by resource wrappers
                typeResource = resolver.getResource(resource.getPath());
                if (typeResource != null &&
                        !typeResource.isResourceType(PagesConstants.NT_COMPONENT)) {
                    // the initialResource is probably an instance of a component not a component itself
                    // in this case we have to switch to the resource of the resource type
                    String resourceType = typeResource.getResourceType();
                    if (StringUtils.isBlank(resourceType) || PRIMARY_TYPE_PATTERN.matcher(resourceType).matches()) {
                        // check a probably present content child if no resource type property found
                        Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
                        if (contentResource != null) {
                            resourceType = contentResource.getResourceType();
                        } else {
                            // is there a hint in the request...
                            // (used if context tools are rendered for the current selection)
                            resourceType = context.getRequest().getParameter(TYPE_HINT_PARAM);
                            if (StringUtils.isBlank(resourceType)) {
                                resourceType = resource.getResourceType();
                            }
                            resourceType = getTypeOfSubtype(resourceType);
                        }
                    }
                    if (StringUtils.isNotBlank(resourceType)) {
                        Matcher matcher = EDIT_SUBTYPE_PATTERN.matcher(resourceType);
                        if (matcher.matches()) {
                            // if type is a subtype use the component type instead
                            resourceType = matcher.group(1);
                        }
                        typeResource = ResolverUtil.getResourceType(typeResource, resourceType);
                    }
                }
            } else {
                // probably a static include of a non existing resource - search for a type hint...
                SlingHttpServletRequest request = context.getRequest();
                String resourceType = (String) request.getAttribute(EDIT_RESOURCE_TYPE_KEY);
                if (StringUtils.isNotBlank(resourceType)) {
                    typeResource = ResolverUtil.getResourceType(resolver, resourceType);
                }
                if (typeResource == null) {
                    String type = request.getParameter(TYPE_HINT_PARAM);
                    if (StringUtils.isNotBlank(type)) {
                        typeResource = ResolverUtil.getResourceType(resolver, type);
                    }
                }
            }
            return typeResource;
        }
    }

    public static String getTypeOfSubtype(String resourceType) {
        Matcher matcher = EDIT_SUBTYPE_PATTERN.matcher(resourceType);
        if (matcher.matches()) {
            // if type is a subtype use the component type instead
            return matcher.group(1);
        }
        return resourceType;
    }

    public EditDialog getEditDialog() {
        if (editDialog == null) {
            editDialog = new EditDialog();
        }
        return editDialog;
    }

    public EditTile getEditTile() {
        if (editTile == null) {
            editTile = new EditTile();
        }
        return editTile;
    }

    // component aspect of AbstractModel

    @Nonnull
    public String getTitleOrName() {
        Component component = getComponent();
        String title = component.getTitle();
        return StringUtils.isNotBlank(title) ? title : component.getName();
    }

    /**
     * the type of a component is the the components resource path relative to the resolver root path
     */
    @Nonnull
    @Override
    public String getType() {
        if (type == null) {
            type = ResourceTypeUtil.relativeResourceType(getContext().getResolver(), getPath());
        }
        return type;
    }

    @Nullable
    public Component getSuperType() {
        Component superType = null;
        String superTypeRef = getProperty(ResourceUtil.PROP_RESOURCE_SUPER_TYPE, "");
        if (StringUtils.isNotBlank(superTypeRef)) {
            BeanContext context = getContext();
            Resource typeResource = ResolverUtil.getResourceType(context.getResolver(), superTypeRef);
            if (typeResource != null) {
                superType = new Component();
                superType.initialize(context, typeResource);
            }
        }
        return superType;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public PagesConstants.ComponentType getComponentType() {
        if (componentType == null) {
            componentType = PagesConstants.ComponentType.typeOf(
                    ResolverUtil.getTypeProperty(getResource(), PN_COMPONENT_TYPE, ""));
        }
        return componentType;
    }

    @Nonnull
    public Thumbnail getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new Thumbnail();
        }
        return thumbnail;
    }

    @Nonnull
    public String getHelpContent() {
        if (helpContent == null) {
            Resource helpRes = getResource().getChild(HELP_PAGE_PATH);
            if (ResourceUtil.isResourceType(helpRes, NODE_TYPE_PAGE)) {
                Resource contentRes = helpRes.getChild(JcrConstants.JCR_CONTENT);
                if (contentRes != null) {
                    helpRes = contentRes;
                }
            }
            helpContent = helpRes != null ? helpRes.getPath() : "";
        }
        return helpContent;
    }

    @Nonnull
    public ComponentPieces getPieces() {
        if (componentPieces == null) {
            componentPieces = new ComponentPieces(getResource());
        }
        return componentPieces;
    }

    // declared properties

    public static final ResourceFilter I18N_PROPERTIES = new ResourceFilter() {

        @Override
        public boolean accept(@Nullable Resource resource) {
            return resource != null && resource.getValueMap().get("i18n", Boolean.FALSE);
        }

        @Override
        public boolean isRestriction() {
            return true;
        }

        @Override
        public void toString(@Nonnull StringBuilder builder) {
            builder.append("i18n");
        }
    };

    @Nonnull
    public Properties getI18nProperties() {
        return getComponentProperties(I18N_PROPERTIES);
    }

    public static final ResourceFilter TEXT_PROPERTIES = new ResourceFilter() {

        @Override
        public boolean accept(@Nullable Resource resource) {
            return resource != null && StringUtils.isNotBlank(resource.getValueMap().get("text", ""));
        }

        @Override
        public boolean isRestriction() {
            return true;
        }

        @Override
        public void toString(@Nonnull StringBuilder builder) {
            builder.append("text");
        }
    };

    @Nonnull
    public Properties getTextProperties() {
        return getComponentProperties(TEXT_PROPERTIES);
    }

    @Nonnull
    public Properties getComponentProperties() {
        return getComponentProperties(ResourceFilter.ALL);
    }

    @Nonnull
    public Properties getComponentProperties(@Nonnull final ResourceFilter filter) {
        Properties result = properties.get(filter.toString());
        if (result == null) {
            result = new Properties(filter);
            properties.put(filter.toString(), result);
        }
        return result;
    }

    @Nullable
    protected Resource getPropertiesDeclaration() {
        if (propertiesDeclaration == null) {
            propertiesDeclaration = getDeclaration("properties");
        }
        return propertiesDeclaration;
    }

    // declared elements

    @Nonnull
    public Elements getComponentElements() {
        return getComponentElements(ResourceFilter.ALL);
    }

    @Nonnull
    public Elements getComponentElements(@Nonnull final ResourceFilter filter) {
        Elements result = elements.get(filter.toString());
        if (result == null) {
            result = new Elements(filter);
            elements.put(filter.toString(), result);
        }
        return result;
    }

    @Nullable
    protected Resource getElementsDeclaration() {
        if (elementsDeclaration == null) {
            elementsDeclaration = getDeclaration("elements");
        }
        return elementsDeclaration;
    }

    // declaration helpers

    @Nullable
    protected Resource getDeclaration(String key) {
        Resource declaration = null;
        Resource content = getResource().getChild(JcrConstants.JCR_CONTENT);
        if (content != null) {
            declaration = content.getChild(key);
        }
        if (declaration == null) {
            Component superType = getSuperType();
            if (superType != null) {
                declaration = superType.getDeclaration(key);
            }
        }
        return declaration;
    }

}
