<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="site" type="com.composum.pages.stage.model.edit.site.SiteModel" mode="none"
             cssBase="composum-pages-site-tools_releases" data-path="@{site.site.path}">
    <div class="composum-pages-tools_actions btn-toolbar">
        <div class="composum-pages-tools_left-actions">
            <label class="tools-title"><cpn:text tagName="span" class="title-text" value="Site Releases"
                                                 i18n="true"/></label>
        </div>
        <div class="composum-pages-tools_right-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-pencil composum-pages-tools_button btn btn-default release-edit"
                        title="${cpn:i18n(slingRequest,'Edit Release properties')}..."></button>
            </div>
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-globe composum-pages-tools_button btn btn-default release-public"
                        title="${cpn:i18n(slingRequest,'Switch Public Release to the selected release (publish)')}..."></button>
                <button type="button"
                        class="fa fa-eye composum-pages-tools_button btn btn-default release-preview"
                        title="${cpn:i18n(slingRequest,'Switch Preview Release to the selected release')}..."></button>
            </div>
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-trash composum-pages-tools_button btn btn-default release-delete"
                        title="${cpn:i18n(slingRequest,'Delete the selected release')}..."></button>
            </div>
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-refresh reload composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}..."></button>
            </div>
        </div>
    </div>
    <div class="${siteCSS}_tools-panel">
        <ul class="${siteCSS}_list">
            <c:forEach items="${site.site.releases}" var="release">
                <sling:include resource="${release.resource}"
                               resourceType="composum/pages/stage/edit/tools/site/releases/release"/>
            </c:forEach>
        </ul>
    </div>
</cpp:element>
