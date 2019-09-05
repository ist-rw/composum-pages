<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:model var="site" type="com.composum.pages.commons.model.Site" mode="none"
           cssBase="composum-pages-stage-edit-site-page-modified">
    <div class="panel panel-default modifiedPages" data-path="${site.path}">
        <div class="panel-heading" role="tab" id="modifiedPagesHead">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse"
                   href="#modifiedPagesPanel" aria-expanded="false"
                   title="${cpn:i18n(slingRequest,'modified but last changes not activated yet')}"
                   aria-controls="modifiedPagesPanel">${cpn:i18n(slingRequest,'Modified Pages')}</a>
            </h4>
            <c:if test="${site.editMode}">
                <div class="btn-group" role="group" aria-label="...">
                    <button class="btn btn-default activate" i18n="true"><i
                            class="fa fa-plaY"></i>${cpn:i18n(slingRequest,'Activate')}</button>
                </div>
            </c:if>
        </div>
        <div id="modifiedPagesPanel" class="panel-collapse collapse in" role="tabpanel"
             aria-labelledby="modifiedPagesHead">
            <div class="${siteCSS}_content panel-body">
                <table class="${siteCSS}_table table table-condensed">
                    <thead class="${siteCSS}_thead">
                    <tr>
                        <th class="_input _page-state"><c:if test="${site.editMode}"><input type="checkbox"
                                                                                            class="${siteCSS}_page-select-all"/></c:if>
                        </th>
                        <th class="_page-path">${cpn:i18n(slingRequest,'Relative Path')}</th>
                        <th class="_page-title">${cpn:i18n(slingRequest,'Title')}</th>
                        <th class="_page-time">${cpn:i18n(slingRequest,'Modification Date')}</th>
                    </tr>
                    </thead>
                    <tbody class="${siteCSS}_tbody">
                    <c:forEach items="${site.modifiedPages}" var="page">
                        <tr class="release-status_${page.releaseStatus.activationState}">
                            <td class="_input _page-state"
                                title="${cpn:i18n(slingRequest,page.releaseStatus.activationState)}"><c:if
                                    test="${site.editMode}"><input type="checkbox"
                                                                   class="${siteCSS}_page-select"
                                                                   data-path="${page.path}"></c:if>
                            </td>
                            <td class="_page-path">
                                <c:choose>
                                    <c:when test="${not empty page.url}">
                                        <a href="${page.url}">${cpn:path(page.siteRelativePath)}</a>
                                    </c:when>
                                    <c:otherwise>
                                        ${cpn:path(page.siteRelativePath)}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="_page-title">${cpn:text(page.title)}</td>
                            <td class="_page-time">${cpn:text(page.lastModifiedString)}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</cpp:model>
