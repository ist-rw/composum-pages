<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:model var="model" type="com.composum.pages.stage.model.edit.site.SiteModel" mode="none"
           cssBase="composum-pages-site-view-page-activated">
    <div class="${modelCssBase}_activated panel panel-default releaseChanges" data-path="${model.site.path}">
        <div class="composum-pages-site-view_heading panel-heading" role="tab" id="releaseChangesHead">
            <h4 title="${cpn:i18n(slingRequest,'content activated with changes since the previous release')}"
                class="composum-pages-site-view_title panel-title">${cpn:i18n(slingRequest,'Activated')}
                <cpp:include resourceType="composum/pages/stage/edit/site/page/type"/>
            </h4>
            <c:if test="${model.editMode}">
                <button class="composum-pages-site-view_button btn btn-default revert"><i
                        class="fa fa-pause"></i>${cpn:i18n(slingRequest,'Revert')}</button>
            </c:if>
            <cpp:include resourceType="composum/pages/stage/edit/site/page/filter" replaceSelectors="activated"/>
        </div>
        <div id="releaseChangesPanel" class="panel-collapse collapse in" role="tabpanel"
             aria-labelledby="releaseChangesHead">
            <div class="${modelCSS}_content panel-body">
                <table class="${modelCSS}_table table table-condensed">
                    <thead class="${modelCSS}_thead">
                    <tr>
                        <th class="_input _page-state"><c:if test="${model.editMode}"><input type="checkbox"
                                                                                             class="${modelCSS}_page-select-all"/></c:if>
                        </th>
                        <th class="_page-path">${cpn:i18n(slingRequest,'Relative Path')}</th>
                        <th class="_page-title">${cpn:i18n(slingRequest,'Title')}</th>
                        <th class="_page-time">${cpn:i18n(slingRequest,'Modification Date')}</th>
                    </tr>
                    </thead>
                    <tbody class="${modelCSS}_tbody">
                    <c:forEach items="${model.releaseChanges}" var="version">
                        <tr class="release-status_${version.contentActivationState}">
                            <td class="_input _page-state"
                                title="${cpn:i18n(slingRequest,version.contentActivationState)}"><c:if
                                    test="${model.editMode}"><input type="checkbox"
                                                                    class="${modelCSS}_page-select"
                                                                    data-path="${cpn:filter(version.path)}"></c:if>
                            </td>
                            <td class="_page-path">
                                <c:choose>
                                    <c:when test="${not empty version.previewUrl}">
                                        <a href="${version.previewUrl}"
                                           target="_blank">${cpn:text(version.siteRelativePath)}</a>
                                    </c:when>
                                    <c:otherwise>
                                        ${cpn:text(version.siteRelativePath)}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="_page-title">${cpn:text(version.title)}</td>
                            <td class="_page-time">${cpn:text(version.lastModifiedString)}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</cpp:model>
