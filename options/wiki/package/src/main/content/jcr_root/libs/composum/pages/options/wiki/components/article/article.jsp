<%@page session="false" pageEncoding="utf-8"%><%--
--%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%><%--
--%><cpp:defineObjects/>
<cpp:element var="article" type="com.composum.pages.options.wiki.model.Article">
    <cpn:text value="${article.title}" tagName="h1" tagClass="wiki-title" />
    <cpn:text value="${article.subtitle}" tagName="p" tagClass="wiki-subtitle" />
    ${article.markup}
</cpp:element>
