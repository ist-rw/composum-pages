<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineObjects/>
<cpp:element var="model" type="com.composum.pages.components.model.reference.Reference">
    <cpp:include test="${model.valid}" path="${model.includePath}" mode="none"/>
    <cpn:div test="${!model.valid && model.editMode}" class="${modelCSS}_placeholder placeholder"><i
            class="fa fa-chain"><span
            class="${modelCSS}_label">${cpn:i18n(slingRequest,'content reference (empty or invalid)')}</span></i></cpn:div>
</cpp:element>