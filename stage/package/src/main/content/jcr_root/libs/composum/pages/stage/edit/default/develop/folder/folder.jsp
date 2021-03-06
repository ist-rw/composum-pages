<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<cpp:editToolbar>
    <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
        <cpp:treeMenu key="more" icon="navicon" label="More..." title="more folder manipulation actions...">
            <cpp:menuItem icon="copy" label="Copy" title="Copy the selected folder"
                          action="window.composum.pages.actions.folder.copy"/>
            <cpp:menuItem icon="id-badge" label="Rename" title="Rename the selected folder"
                          action="window.composum.pages.actions.folder.rename"/>
            <cpp:menuItem icon="arrows-alt" label="Move" title="Move the selected folder"
                          action="window.composum.pages.actions.folder.move"/>
            <cpp:menuItem icon="trash" label="Delete" title="Delete the selected folder"
                          action="window.composum.pages.actions.folder.delete"/>
        </cpp:treeMenu>
        <cpp:treeAction icon="edit" label="Rename" title="Edit folder title / type"
                        action="window.composum.pages.actions.component.folder.edit"/>
    </div>
    <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
        <cpp:treeMenu key="insert" icon="plus" label="Insert" title="insert a new content element">
            <cpp:menuItem icon="puzzle-piece" label="Create Component" title="Create a new Component"
                          action="window.composum.pages.actions.component.create"/>
            <cpp:menuItem icon="image" label="File" title="upload a file as direct child of the selected folder"
                          action="window.composum.pages.actions.component.folder.insertFile"/>
            <cpp:menuItem icon="folder-open" label="Folder"
                          title="insert a new folder as direct child of the selected folder"
                          action="window.composum.pages.actions.component.folder.insertFolder"/>
        </cpp:treeMenu>
        <cpp:treeAction icon="paste" label="Paste" title="Paste element as child of the selected folder"
                        action="window.composum.pages.actions.folder.paste"/>
    </div>
</cpp:editToolbar>
