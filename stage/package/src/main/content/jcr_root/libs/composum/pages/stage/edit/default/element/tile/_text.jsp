<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:model var="element" type="com.composum.pages.commons.model.GenericModel"
           cssBase="composum-pages-component-tile">
    <sling:call script="icon.jsp"/>
    <div class="${elementCssBase}_title">${cpn:i18n(slingRequest,element.title)}</div>
    <cpn:text value="${element.name}" format="{Message}({0})" class="${elementCssBase}_name"/>
    <cpn:text value="${element.component.typeHint}" class="${elementCssBase}_type"/>
    <cpn:text value="${element.description}" class="${elementCssBase}_description"/>
</cpp:model>
