<%@page session="false" pageEncoding="utf-8" %><%--
--%><%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%--
--%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %><%--
--%><%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %><%--
--%><cpp:defineObjects/><%--
--%><jsp:useBean id="searchresult" type="com.composum.pages.commons.service.SearchService.Result" scope="request" />
<p>
    <cpn:link href="${searchresult.targetUrl}" classes="title"><cpn:text value="${searchresult.title}"/></cpn:link>
    <span class="score">(Score ${searchresult.score})</span><br/>
    <span class="excerpt">${searchresult.excerpt}</span>
</p>
