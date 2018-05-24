<%@page session="false" pageEncoding="utf-8" %><%--
--%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %><%--
--%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%--
--%><cpp:defineFrameObjects/>
<cpp:editDialog var="article" type="com.composum.pages.wiki.model.Article"
                title="Edit Wiki Article">
    <cpp:editDialogTab tabId="markup" label="Markup Text">
        <cpp:widget label="Type" property="type" type="select" options="${article.types}" rules="mandatory"/>
        <cpp:widget label="Markup" property="markup" type="codearea" height="600" i18n="true"/>
    </cpp:editDialogTab>
    <cpp:editDialogTab tabId="props" label="Properties">
        <cpp:widget label="Title" property="jcr:title" name="title" type="textfield" i18n="true"/>
        <cpp:widget label="Subtitle" property="subtitle" type="textfield" i18n="true"/>
        <cpp:widget label="Category" property="category" type="textfield"/>
        <cpp:widget label="Markup File" property="reference" type="pathfield" i18n="true"/>
    </cpp:editDialogTab>
</cpp:editDialog>
