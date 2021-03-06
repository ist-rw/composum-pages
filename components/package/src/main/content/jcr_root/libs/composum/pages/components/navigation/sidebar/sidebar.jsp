<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineObjects/>
<cpp:element var="menu" type="com.composum.pages.components.model.navigation.SidebarMenu" mode="none"
             cssBase="composum-pages-components-navigation-sidebar">
    <c:if test="${not empty menu.menuItems}">
        <ul class="${menuCSS}_list nav" role="menu">
            <c:forEach items="${menu.menuItems}" var="item">
                <cpp:include path="${item.content.path}" resourceType="composum/pages/components/navigation/menuitem"
                             replaceSelectors="sidebar"/>
            </c:forEach>
        </ul>
    </c:if>
</cpp:element>
