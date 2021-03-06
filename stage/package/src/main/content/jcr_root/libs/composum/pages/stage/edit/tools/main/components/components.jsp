<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="components" type="com.composum.pages.stage.model.edit.page.Components" mode="none"
             cssAdd="composum-pages-tools">
    <div class="composum-pages-tools_actions btn-toolbar">
        <div class="composum-pages-tools_left-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <div class="input-group">
                    <span class="composum-pages-tools_search-reset input-group-addon fa fa-times-circle"></span>
                    <input class="composum-pages-tools_search-field form-control" type="text"
                           title="${cpn:i18n(slingRequest,'Search Term')}"
                           placeholder="${cpn:i18n(slingRequest,'search...')}"/>
                    <span class="composum-pages-tools_search-action input-group-addon fa fa-search"></span>
                </div>
            </div>
        </div>
        <div class="composum-pages-tools_right-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button" class="filter-toggle fa fa-filter composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Filter')}"></button>
                <cpp:include replaceSelectors="filter"/>
                <button type="button"
                        class="fa fa-refresh ${componentsCSS}_reload composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"><cpn:text
                        tagName="span" class="composum-pages-tools_button-label"
                        i18n="true">Reload</cpn:text></button>
            </div>
        </div>
    </div>
    <div class="composum-pages-tools_panel">
        <div class="${componentsCSS}_components-view"></div>
    </div>
</cpp:element>
