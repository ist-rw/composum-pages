<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:model var="file" type="com.composum.pages.commons.model.File" mode="none"
           cssBase="composum-pages-stage-file_tile">
    <div class="${fileCssBase} ${file.mimeTypeCss}" title="${file.filePath}">
        <div class="${fileCssBase}_frame">
            <div class="${fileCssBase}_properties">
                <cpn:text value="${file.fileName}" class="${fileCssBase}_name"></cpn:text>
                <cpn:text value="${file.mimeType}" class="${fileCssBase}_mime-type"></cpn:text>
            </div>
        </div>
    </div>
</cpp:model>
