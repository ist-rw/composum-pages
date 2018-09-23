<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:editDialog var="list" type="com.composum.pages.components.model.listtype.ListType"
                title="@{dialog.selector=='create'?'Create a List':'Edit List'}">
    <cpp:widget label="Choose ordered or unordered list" property="listType" type="select" options="ul:unordered,ol:ordered"/>
</cpp:editDialog>
