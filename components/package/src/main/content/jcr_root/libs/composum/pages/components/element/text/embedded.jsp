<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineObjects/>
<cpp:model var="model" type="com.composum.pages.components.model.text.Text">
    <c:choose>
        <c:when test="${model.valid}">
            <sling:call script="textelements.jsp"/>
        </c:when>
        <c:otherwise>
            <cpp:include test="${model.editMode}" replaceSelectors="placeholder"/>
        </c:otherwise>
    </c:choose>
</cpp:model>