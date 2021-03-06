<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineObjects/>
<sling:call script="meta.jsp"/>
<title>${cpn:text(currentPage.title)}</title>
<cpn:link test="${currentPage.home||!currentPage.canonicalRequest}"
          tagName="link" rel="canonical" href="${currentPage.canonicalUrl}"/>
<sling:call script="hreflang.jsp"/>
<cpn:clientlib type="link" category="${currentPage.viewClientlibCategory}"/>
<cpn:clientlib type="css" category="${currentPage.viewClientlibCategory}"/>
<cpn:clientlib type="css" test="${currentPage.editMode}" category="${currentPage.editClientlibCategory}"/>
