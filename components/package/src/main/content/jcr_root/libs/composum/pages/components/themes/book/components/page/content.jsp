<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineObjects/><%
    slingRequest.setAttribute("NARROW_MAIN", true);
%>
<div class="composum-pages-components-page_content">
    <div class="composum-pages-components-page_content_top">
        <cpp:include path="top" resourceType="composum/pages/components/container/iparsys"/>
    </div>
    <cpp:include resourceType="composum/pages/components/navigation/breadcrumbs"/>
    <div class="composum-pages-components-page_content-row row">
        <a class="composum-pages-components-page_content_nav_toggle fa fa-bars" href="#"></a>
        <nav class="composum-pages-components-page_content_nav col-xl-2 col-md-3 col-sm-12">
            <cpp:include resourceType="composum/pages/components/navigation/sidebar" replaceSelectors="showroot"/>
        </nav>
        <main class="composum-pages-components-page_content_main col-xl-10 col-md-9 col-sm-12">
            <div class="composum-pages-components-page_main-body">
                <cpp:include path="main" resourceType="composum/pages/components/container/parsys"/>
            </div>
        </main>
    </div>
    <div class="composum-pages-components-page_content_bottom">
        <cpp:include path="bottom" resourceType="composum/pages/components/container/iparsys"/>
    </div>
</div>
