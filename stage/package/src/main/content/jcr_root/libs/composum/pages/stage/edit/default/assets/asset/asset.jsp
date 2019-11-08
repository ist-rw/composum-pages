<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:editToolbar>
    <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
        <cpp:treeMenu key="more" icon="navicon" label="More..." title="more file manipulation actions...">
            <cpp:menuItem icon="id-badge" label="Rename" title="Rename the selected asset"
                          action="window.composum.pages.actions.file.rename"/>
            <cpp:menuItem icon="arrows-alt" label="Move" title="Move the selected asset"
                          action="window.composum.pages.actions.file.move"/>
        </cpp:treeMenu>
        <cpp:treeAction icon="upload" label="Upload" title="Upload asset original"
                        action="window.composum.assets.pages.actions.asset.upload"/>
    </div>
    <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
        <cpp:treeAction icon="copy" label="Copy" title="Copy the selected asset"
                        action="window.composum.pages.actions.folder.copy"/>
        <cpp:treeAction icon="trash" label="Delete" title="Delete the selected asset"
                        action="window.composum.assets.pages.actions.asset.delete"/>
    </div>
    <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
        <cpp:treeAction icon="sliders" label="Configure" title="Configure asset"
                        action="window.composum.assets.pages.actions.asset.config"/>
    </div>
</cpp:editToolbar>