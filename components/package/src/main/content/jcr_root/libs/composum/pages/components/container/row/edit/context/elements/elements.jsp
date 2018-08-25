<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:container var="row" type="com.composum.pages.components.model.container.Row" mode="none" tagName="none"
               cssBase="composum-pages-stage-edit-tools-container-elements">
    <ul class="${rowCssBase}_list">
        <c:forEach items="${row.columns}" var="column" varStatus="loop">
            <li class="${rowCssBase}_element" draggable="true"
                data-name="column-${loop.index}" data-path="${row.path}/column-${loop.index}"
                data-type="composum/pages/components/container/row/column">
                <cpp:include path="column-${loop.index}" resourceType="composum/pages/components/container/row/column"
                             subtype="edit/tile"/>
            </li>
        </c:forEach>
    </ul>
</cpp:container>
