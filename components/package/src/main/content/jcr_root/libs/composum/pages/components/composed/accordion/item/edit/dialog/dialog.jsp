<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:editDialog title="@{dialog.selector=='create'?'Create an Item':'Accordion Item'}">
    <cpp:widget label="Title" property="title" type="textfield" i18n="true" required="true"/>
    <div class="col col-xs-4">
        <cpp:widget type="checkbox" label="Open" property="initialOpen"/>
    </div>
</cpp:editDialog>
