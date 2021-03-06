package com.composum.pages.commons.taglib;

import com.composum.pages.commons.util.ResourceTypeUtil;
import com.composum.pages.commons.util.TagCssClasses;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.composum.pages.commons.taglib.AbstractPageTag.STAGE_COMPONENT_BASE;
import static com.composum.pages.commons.util.TagCssClasses.cssOfType;
import static com.composum.sling.cpnl.TagBase.TAG_NONE;

/**
 * the EditDialogTag creates the HTML code for an edit dialog of a component
 */
@SuppressWarnings("JavaDoc")
public class EditDialogTag extends AbstractEditTag {

    public static final String DIALOG_VAR = "dialog";
    public static final String DIALOG_CSS_VAR = DIALOG_VAR + "CSS";
    public static final String DIALOG_CSSBASE_VAR = DIALOG_VAR + "CssBase";

    public static final String SELECTOR_CHANGE = "change";
    public static final String SELECTOR_CREATE = "create";
    public static final String SELECTOR_DELETE = "delete";
    public static final String SELECTOR_EDIT = "edit";
    public static final String SELECTOR_GENERIC = "generic";
    public static final String SELECTOR_NEW = "new";
    public static final String SELECTOR_WIZARD = "wizard";
    public static final List<String> KNOWN_SELECTORS = Arrays.asList(
            SELECTOR_EDIT,
            SELECTOR_CREATE,
            SELECTOR_NEW,
            SELECTOR_DELETE,
            SELECTOR_CHANGE,
            SELECTOR_GENERIC,
            SELECTOR_WIZARD
    );
    public static final String DEFAULT_SELECTOR = SELECTOR_EDIT;

    public static final String DEFAULT_CSS_BASE = "composum-pages-stage-edit-dialog";

    public static final String DIALOG_PATH = "/edit/dialog";

    public static final String CUSTOM_POST_SERVLET_ACTION = "Custom-POST";

    public static final String ATTR_SUBMIT_LABEL = "submitLabel";
    public static final String DEFAULT_SUBMIT_LABEL = "Submit";

    public static final String PAGES_EDIT_VALIDATION = PAGES_EDIT_DATA + "-validation";
    public static final String PAGES_EDIT_SUCCESS_EVENT = PAGES_EDIT_DATA + "-success";

    private transient String dialogId;

    protected String title;
    protected String titleValue;

    protected String selector;
    protected String selectorValue;

    protected boolean languageContext = true;

    protected String submit;
    protected String submitLabel;
    private transient FormAction action;

    protected String validation;
    protected String validationValue;
    protected String successEvent;

    @Override
    protected void clear() {
        successEvent = null;
        validationValue = null;
        validation = null;
        action = null;
        submit = null;
        submitLabel = null;
        languageContext = true;
        selectorValue = null;
        selector = null;
        titleValue = null;
        title = null;
        dialogId = null;
        super.clear();
    }

    // tag attributes

    public String getTitle() {
        if (titleValue == null) {
            titleValue = i18n(eval(title, "Edit Element"));
        }
        return titleValue;
    }

    public void setTitle(String text) {
        title = text;
    }

    public boolean isDisabledSet() {
        return getDisabledValue();
    }

    /**
     * tag attribute - switch the language context hint on/off
     *
     * @param languageContext boolean; show the hint (default: true)
     */
    public void setLanguageContext(boolean languageContext) {
        this.languageContext = languageContext;
    }

    public boolean isHasLanguageContext() {
        return languageContext;
    }

    public String getCreateNameHint() {
        String hint = null;
        String type = getResourceType();
        if (StringUtils.isNotBlank(type)) {
            hint = type.substring(type.lastIndexOf("/") + 1);
        }
        if (StringUtils.isBlank(hint)) {
            type = getPrimaryType();
            if (StringUtils.isNotBlank(type)) {
                hint = type.substring(type.lastIndexOf(":") + 1);
            }
        }
        return StringUtils.isNotBlank(hint) ? hint : "element";
    }

    /**
     * if this returns 'true' the hidden resource type property value must be inserted in the form
     */
    public boolean isUseResourceType() {
        return useCreationType(getResourceType());
    }

    /**
     * if this returns 'true' the hidden primary type property value must be inserted in the form
     */
    public boolean isUsePrimaryType() {
        return useCreationType(getPrimaryType());
    }

    public boolean useCreationType(String type) {
        return StringUtils.isNotBlank(type) && !TAG_NONE.equalsIgnoreCase(type) && isCreateOrSynthetic();
    }

    public boolean isCreateOrSynthetic() {
        return SELECTOR_CREATE.equalsIgnoreCase(getSelector())
                || ResourceTypeUtil.isSyntheticResource(getModelResource(context));
    }

    // the dialog variation selector

    /**
     * returns the first not empty value of the cascade, as key to select the right dialog snippets:
     * <ul>
     * <li>the eval() result of the declared selector</li>
     * <li>the selector string of the request</li>
     * <li>the default 'edit' selector (main snippet)</li>
     * </ul>
     *
     * @see /libs/composum/pages/stage/edit/dialog/...
     */
    public String getSelector() {
        if (selectorValue == null) {
            selectorValue = eval(selector, "");
            if (StringUtils.isBlank(selectorValue)) {
                selectorValue = request.getRequestPathInfo().getSelectorString();
                if (StringUtils.isNotBlank(selectorValue) && !KNOWN_SELECTORS.contains(selectorValue)) {
                    selectorValue = null;
                }
                if (StringUtils.isBlank(selectorValue)) {
                    selectorValue = DEFAULT_SELECTOR;
                }
            }
        }
        return selectorValue;
    }

    /**
     * adds the set of initial default tag attributes
     */
    protected void defaultAttributes(Map<String, Object> attributeSet) {
        attributeSet.put("data-backdrop", "static");
        attributeSet.put("data-keyboard", Boolean.TRUE);
    }

    /**
     * filters dynamic attributes for special purposes:
     * <ul>
     * <li>'submit' button label for the 'generic' dialog</li>
     * </ul>
     */
    @Override
    protected boolean acceptDynamicAttribute(String key, Object value) throws JspException {
        if (ATTR_SUBMIT_LABEL.equals(key)) {
            submitLabel = (String) value;
            return false;
        } else {
            return super.acceptDynamicAttribute(key, value);
        }
    }

    /**
     * tag attribute - defines the optional selector for the rendering of the dialog component;
     * this selector string is mainly used to select the right dialog 'start' and 'end' snippets
     *
     * @param selector the selector key
     * @see /libs/composum/pages/stage/edit/dialog/...
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }

    // dialog submit action ...

    /**
     * @return the localized submit button label mainly for the generic dialog type (selector)
     */
    public String getSubmitLabel() {
        return i18n(StringUtils.isNotBlank(submitLabel) ? submitLabel : DEFAULT_SUBMIT_LABEL);
    }

    public void setSubmit(String actionKey) {
        submit = actionKey;
        switch (actionKey) {
            case "SlingPostServlet":
            case SLING_POST_SERVLET_ACTION:
                action = new SlingPostServletAction();
                break;
            default:
                action = new CustomPostAction(actionKey);
                break;
        }
    }

    // validation request

    public String getValidation() {
        return validation;
    }

    public String getValidationValue() {
        if (validation != null) {
            if (validationValue == null) {
                validationValue = eval(validation, "");
            }
        }
        return validationValue;
    }

    public void setValidation(String requestRule) {
        this.validation = requestRule;
    }

    // post dialog event

    public String getSuccessEvent() {
        return successEvent;
    }

    public void setSuccessEvent(String eventKey) {
        this.successEvent = eventKey;
    }

    //
    //
    //

    public String getPropertyPath(String relativePath, String name) {
        return getAction().getPropertyPath(relativePath, name);
    }

    public String getDialogId() {
        if (dialogId == null) {
            String type = resource.getResourceType();
            dialogId = cssOfType(type);
        }
        return dialogId;
    }

    @Override
    protected void collectCssClasses(TagCssClasses.CssSet collection) {
        super.collectCssClasses(collection);
        collection.add(getCssBase() + "_action_" + getAction().getName().toLowerCase());
        String selector = getSelector();
        if (StringUtils.isNotBlank(selector)) {
            collection.add(getCssBase() + "_selector_" + selector);
        }
        collection.add(getCssBase() + "_action_" + getAction().getName().toLowerCase());
        collection.add("dialog");
        collection.add("modal");
        collection.add("fade");
    }

    @Override
    protected void collectAttributes(Map<String, Object> attributeSet) {
        super.collectAttributes(attributeSet);
        String value;
        attributeSet.put("role", "dialog");
        attributeSet.put("aria-hidden", "true");
        if (StringUtils.isNotBlank(value = getValidationValue())) {
            attributeSet.put(PAGES_EDIT_VALIDATION, value);
        }
        if (StringUtils.isNotBlank(value = getSuccessEvent())) {
            attributeSet.put(PAGES_EDIT_SUCCESS_EVENT, value);
        }
    }

    // tag rendering

    protected String getSnippetResourceType() {
        return STAGE_COMPONENT_BASE + DIALOG_PATH;
    }

    @Override
    public int doStartTag() throws JspException {
        if (StringUtils.isBlank(cssBase)) {
            cssBase = DEFAULT_CSS_BASE;
        }
        return super.doStartTag();
    }

    @Override
    protected void prepareTagStart() {
        setAttribute(DIALOG_VAR, this, PageContext.REQUEST_SCOPE);
        if (StringUtils.isNotBlank(cssBase)) {
            setAttribute(DIALOG_CSS_VAR, cssBase, PageContext.REQUEST_SCOPE);
            setAttribute(DIALOG_CSSBASE_VAR, cssBase, PageContext.REQUEST_SCOPE);
        }
    }

    @Override
    protected void renderTagStart() throws IOException {
        includeSnippet(getSnippetResourceType(), getSelector() + "-dialog-start");
    }

    @Override
    protected void renderTagEnd() throws IOException {
        includeSnippet(getSnippetResourceType(), getSelector() + "-dialog-end");
    }

    @Override
    protected void finishTagEnd() {
    }

    // dialog submit action ...

    public FormAction getDefaultAction() {
        return new SlingPostServletAction();
    }

    public FormAction getAction() {
        if (action == null) {
            action = getDefaultAction();
        }
        return action;
    }

    public class CustomPostAction implements FormAction {

        protected final String uri;

        public CustomPostAction(String uri) {
            this.uri = uri;
        }

        @Override
        @Nonnull
        public String getName() {
            return CUSTOM_POST_SERVLET_ACTION;
        }

        @Override
        @Nonnull
        public String getUrl() {
            return request.getContextPath() + eval(uri, "/");
        }

        @Override
        @Nonnull
        public String getMethod() {
            return "POST";
        }

        @Override
        @Nonnull
        public String getEncType() {
            return "multipart/form-data";
        }

        @Override
        @Nonnull
        public String getPropertyPath(String relativePath, String name) {
            return getI18nPath(relativePath, name);
        }
    }
}
