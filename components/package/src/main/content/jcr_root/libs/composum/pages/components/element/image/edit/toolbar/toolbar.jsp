<%@page session="false" pageEncoding="utf-8" %><%--
--%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%--
--%><cpp:defineFrameObjects/>
<cpp:editToolbar var="element" type="com.composum.pages.commons.model.Element"
                 cssBase="composum-pages-stage-edit-toolbar">
    <cpp:editAction icon="edit" label="Edit" title="Edit the selected Element"
                    action="window.composum.pages.actions.element.edit"/>
    <cpp:editAction icon="image" label="Show Asset" title="Show Asset"
                    action="window.composum.pages.actions.asset.show"/>
    <cpp:editAction icon="trash" label="Delete" title="Delete the selected Element"
                    action="window.composum.pages.actions.element.delete"/>
</cpp:editToolbar>
