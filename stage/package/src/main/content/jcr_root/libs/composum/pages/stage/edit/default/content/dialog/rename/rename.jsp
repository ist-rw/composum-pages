<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:editDialog var="model" type="com.composum.pages.commons.model.GenericModel" selector="generic"
                title="Rename Content" submitLabel="Rename" languageContext="false">
    <div class="panel panel-info tile-panel">
        <div class="panel-body">
            <cpp:include resource="${model.resource}" subtype="edit/tile" replaceSelectors="wide"/>
        </div>
    </div>
    <cpp:widget name="path" value="${model.path}" type="hidden"/>
    <cpp:widget label="New Name" name="name" value="${model.name}" type="textfield"
                required="true" pattern="/^[\\p{L}_][\\p{N}\\p{L} _-]*$/u"
                pattern-hint="a letter followed by letters, digits, blanks or any of '_-'"/>
</cpp:editDialog>
