<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<cpp:defineFrameObjects/>
<div${dialog.attributes}>
    <div class="modal-dialog">
        <div class="modal-content form-panel">
            <form class="widget-form ${dialogCssBase}_form" method="${dialog.action.method}"
                  action="${dialog.action.url}" enctype="${dialog.action.encType}">
                <div class="modal-header ${dialogCssBase}_header">
                    <button type="button" class="${dialogCssBase}_button-close fa fa-close"
                            data-dismiss="modal" aria-label="Close"></button>
                    <h4 class="modal-title ${dialogCssBase}_dialog-title">
                        ${cpn:text(dialog.title)}
                    </h4>
                </div>
                <div class="modal-body ${dialogCssBase}_content">
                    <div class="${dialogCssBase}_messages messages">
                        <div class="panel panel-${dialog.alertKey}">
                            <div class="panel-heading">${dialog.alertText}</div>
                            <div class="panel-body hidden"></div>
                        </div>
                    </div>
                    <ul class="${dialogCssBase}_tabs nav nav-tabs composum-commons-form-tab-nav">
                    </ul>
                    <input name="_charset_" type="hidden" value="UTF-8" class="${dialogCssBase}_hidden"/>
                    <input name="path" type="hidden" value="${dialog.editPath}" class="${dialogCssBase}_hidden ${dialogCssBase}_path"/>
                    <div class="${dialogCssBase}_tabbed-content composum-commons-form-tabbed">
<!-- start of dialog content -->
