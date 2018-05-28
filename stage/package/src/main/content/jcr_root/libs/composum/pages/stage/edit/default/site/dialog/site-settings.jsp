<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<div class="row">
    <div class="col-xs-8">
        <cpp:widget label="Title" property="jcr:title" type="textfield"/>
    </div>
    <div class="col-xs-4">
        <cpp:widget label="Publish Policy" name="publicMode" type="select"
                    options=",inPlace:In-Place replication,versions:Versions resolver,live:Live imediately"/>
    </div>
</div>
<div class="row">
    <div class="col-xs-12">
        <cpp:widget label="Description" property="jcr:description" type="richtext" i18n="false"/>
    </div>
</div>
