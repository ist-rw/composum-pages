package com.composum.pages.components.model.navigation;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.Page;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static com.composum.pages.commons.PagesConstants.PN_TITLE;
import static com.composum.pages.commons.PagesConstants.PN_TITLE_KEYS;
import static com.composum.pages.commons.PagesConstants.PROP_NAV_TITLE;

public class Menuitem extends Page {

    private static final Logger LOG = LoggerFactory.getLogger(Menuitem.class);

    public static final String[] PROP_TITLE_KEYS = new String[]{PROP_NAV_TITLE, ResourceUtil.PROP_TITLE, PN_TITLE};

    public static final String MENU_ITEM_CSS_BASE_TYPE = "composum/pages/components/navigation/menuitem";

    private transient Boolean redirect;
    private transient Boolean menuOnly;
    private transient Menu submenu;

    private transient String pageTitle;

    public Menuitem() {
    }

    public Menuitem(BeanContext context, Resource resource) {
        super(context, resource);
    }

    @Override
    protected String[] getTitleKeys() {
        return PROP_TITLE_KEYS;
    }

    @Nonnull
    @Override
    public String getTitle() {
        String title = super.getTitle();
        return StringUtils.isNotBlank(title) ? title : getName();
    }

    @Nonnull
    public String getPageTitle() {
        if (pageTitle == null) {
            pageTitle = getProperty(getLocale(), "", PN_TITLE_KEYS);
        }
        return StringUtils.isNotBlank(pageTitle) ? pageTitle : getTitle();
    }

    @Override
    protected String getCssBaseType() {
        return MENU_ITEM_CSS_BASE_TYPE;
    }

    public String getCssClasses() {
        StringBuilder cssClasses = new StringBuilder();
        Page currentPage = getCurrentPage();
        if (currentPage != null) {
            String currentPagePath = currentPage.getPath();
            String menuitemPath = resource.getPath();
            if (menuitemPath.equals(currentPagePath)) {
                cssClasses.append(" current");
            }
            if (isActive()) {
                cssClasses.append(" active");
            }
        }
        return cssClasses.toString();
    }

    public boolean isNavRoot() {
        return getProperty(PagesConstants.PROP_IS_NAV_ROOT, null, Boolean.FALSE);
    }

    public boolean isActive() {
        Page currentPage = getCurrentPage();
        if (currentPage != null) {
            String currentPagePath = currentPage.getPath();
            String menuitemPath = resource.getPath();
            return menuitemPath.equals(currentPagePath) || currentPagePath.startsWith(menuitemPath + "/");
        }
        return false;
    }

    public boolean getIsCurrent() {
        Page currentPage = getCurrentPage();
        if (currentPage != null) {
            String currentPagePath = currentPage.getPath();
            String menuitemPath = resource.getPath();
            return menuitemPath.equals(currentPagePath);
        }
        return false;
    }

    public boolean isMenuOnly() {
        if (menuOnly == null) {
            String targetUrl = getSlingTargetUrl();
            menuOnly = targetUrl != null && StringUtils.isBlank(targetUrl);
        }
        return menuOnly;
    }

    public boolean isRedirect() {
        if (redirect == null) {
            String targetUrl = getSlingTargetUrl();
            redirect = StringUtils.isNotBlank(targetUrl);
        }
        return redirect;
    }

    @Override
    @Nonnull
    public String getUrl() {
        String targetUrl = getSlingTargetUrl();
        return StringUtils.isNotBlank(targetUrl) ? targetUrl : super.getUrl();
    }

    public boolean isSubmenu() {
        return !getMenu().isEmpty();
    }

    public Menu getMenu() {
        if (submenu == null) {
            submenu = buildMenu();
        }
        return submenu;
    }

    protected Menu buildMenu() {
        return new Menu(context, resource);
    }
}
