<%@page session="false" pageEncoding="UTF-8" %><%--
--%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %><%--
--%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%--
--%><%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%--
--%><cpp:defineFrameObjects/>
<div class="${widgetCssBase}_${widget.widgetType} ${widgetCssBase}_${widget.cssName} form-group">
    <sling:call script="label.jsp"/>
    <div class="${widgetCssBase}_wrapper widget site-select-widget">
        <div class="${widgetCssBase}_toolbar">
            <div class="${widgetCssBase}_search">
            </div>
        </div>
        <c:if test="${empty widget.model.sites}">
            <div class="${widgetCssBase}_empty">
                <cpn:text class="${widgetCssBase}_paragraph alert alert-warning"
                          value="no Sites available" i18n="true"/>
            </div>
        </c:if>
        <ul class="${widgetCssBase}_list">
            <c:forEach items="${widget.model.sites}" var="site">
                <li class="${widgetCssBase}_site">
                    <input type="radio" name="${widget.name}" value="${site.path}" class="${widgetCssBase}_radio"/>
                    <cpp:include resource="${site.resource}" subtype="edit/tile" replaceSelectors="select"/>
                </li>
            </c:forEach>
        </ul>
    </div>
</div>

