<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<div class="row">
    <div class="col col-xs-8">
    </div>
    <div class="col col-xs-4">
        <cpp:widget label="Alignment" property="textAlignment" type="select"
                    options="left,right,center,justify" default="left"/>
    </div>
</div>
