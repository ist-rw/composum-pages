(function (window) {
    window.composum = window.composum || {};
    window.composum.pages = window.composum.pages || {};
    window.composum.pages.tree = window.composum.pages.tree || {};

    (function (tree, pages, core) {
        'use strict';

        tree.const = _.extend(tree.const || {}, {
            css: {
                tools: 'composum-pages-tools',
                main: 'composum-pages-stage-edit-tools-main',
                _: {
                    pages: '-pages',
                    assets: '-assets',
                    develop: '-develop',
                    tree: '_tree',
                    treePanel: '_tree-panel',
                    actions: '_actions',
                    refresh: '_refresh'
                },
                preview: 'tree-panel-preview'
            },
            actions: {
                css: 'composum-pages-tools_left-actions',
                url: '/bin/cpm/pages/edit.treeActions.html',
                assets: '/bin/cpm/pages/assets.treeActions.html',
                develop: '/bin/cpm/pages/develop.treeActions.html'
            },
            tile: {
                url: '/bin/cpm/pages/edit.editTile.html',
                asset: '/bin/cpm/pages/assets.editTile.html'
            },
            pages: {
                css: {
                    base: 'composum-pages-stage-edit-tools-main-pages'
                }
            },
            assets: {
                css: {
                    base: 'composum-pages-stage-edit-tools-main-assets'
                }
            },
            develop: {
                css: {
                    base: 'composum-pages-stage-edit-tools-main-develop'
                }
            }
        });

        tree.ToolsTree = core.components.Tree.extend({

            initialize: function (options) {
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            /**
             * change tree selection according to a selected path (tree is secondary view)
             */
            selectedNode: function (path, callback) {
                this.suppressEvent = true;
                this.selectNode(path, _.bind(function (path) {
                    this.suppressEvent = false;
                    if (_.isFunction(callback)) {
                        callback(path);
                    }
                }, this));
            },

            onPathSelectedFailed: function (path) {
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onPathSelectedFailed(' + path + ')');
                }
                var node;
                if (!(node = this.getSelectedTreeNode()) && (path = core.getParentPath(path)) && path !== '/') {
                    this.selectedNode(path, _.bind(this.checkSelectedPath, this));
                }
            },

            onPathSelected: function (event, path) {
                if (core.components.Tree.prototype.onPathSelected.apply(this, [event, path])) {
                    this.$el.trigger("node:selected", [path]);
                }
            },

            /**
             * refresh the data and tree node state of one node (no structure change refresh!)
             * @param refOrPath the reference or the path of the repository resource
             */
            refreshTreeNodeState: function (refOrPath) {
                if (refOrPath) {
                    var path = refOrPath.path ? refOrPath.path : refOrPath;
                    // load data and replace the 'original' data store of the node
                    core.getJson(this.dataUrlForPath(path), _.bind(function (data) {
                        var nodeId = this.nodeId(path);
                        var node = this.jstree.get_node(nodeId);
                        if (this.log.getLevel() <= log.levels.DEBUG) {
                            this.log.debug(this.nodeIdPrefix + 'tree.refreshTreeNodeState(' + path + ')');
                        }
                        if (node) {
                            if (data.children) {
                                // drop children - not for structure change refresh!
                                delete data.children;
                            }
                            node.original = data; // replace 'original' data store and redraw...
                            this.refreshNodeState(this.jstree.get_node(nodeId, true), node);
                        }
                    }, this));
                }
            }
        });

        tree.ContentTree = tree.ToolsTree.extend({

            initialize: function (options) {
                var e = pages.const.event;
                options = _.extend(options || {}, {
                    dragAndDrop: {
                        check_while_dragging: false,
                        copy: false,
                        drag_selection: false,
                        inside_pos: 'last',
                        is_draggable: _.bind(this.nodeIsDraggable, this),
                        large_drag_target: true,
                        large_drop_target: true,
                        open_timeout: 0,
                        touch: 'selected',
                        use_html5: true
                    }
                });
                tree.ToolsTree.prototype.initialize.apply(this, [options]);
                this.jstree.element
                    .on('dragstart.' + this.nodeIdPrefix + 'tree', _.bind(this.onNodeDragStart, this))
                    .on('dragend.' + this.nodeIdPrefix + 'tree', _.bind(this.onNodeDragEnd, this));
                $(document)
                    .on('dnd_move.vakata.' + this.nodeIdPrefix + 'tree', _.bind(this.onDragMove, this))
                    .on(e.dnd.finished + '.' + this.nodeIdPrefix + 'tree', _.bind(this.onDragFinished, this));
            },

            /**
             * add the release status CSS class for the page
             */
            refreshNodeState: function ($node, node) {
                node = tree.ToolsTree.prototype.refreshNodeState.apply(this, [$node, node]);
                if (node.original.release && node.original.release.status) {
                    ['initial', 'activated', 'modified', 'deactivated'].forEach(function (value) {
                        $node.removeClass('release-status_' + value);
                    });
                    $node.addClass('release-status_' + node.original.release.status);
                }
                return node;
            },

            onContentInserted: function (event, parentRef, /*optional*/ resultRef) {
                var path = parentRef && parentRef.path ? parentRef.path : parentRef;
                if (path) {
                    if (!resultRef && path.indexOf('/content/') === 0) {
                        path = core.getParentPath(path);
                    }
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        this.log.debug(this.nodeIdPrefix + 'tree.onContentInserted(' + path + ')');
                    }
                    this.onPathInserted(event, path, resultRef && resultRef.name ? resultRef.name : '*');
                }
            },

            onContentMoved: function (event, oldRefOrPath, newRefOrPath) {
                var oldPath = oldRefOrPath && oldRefOrPath.path ? oldRefOrPath.path : oldRefOrPath;
                var newPath = newRefOrPath && newRefOrPath.path ? newRefOrPath.path : newRefOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentMoved(' + oldPath + ' ->' + newPath + ')');
                }
                this.onPathMoved(event, oldPath, newPath);
            },

            onContentChanged: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentChanged(' + path + ')');
                }
                this.onPathChanged(event, path);
            },

            onContentDeleted: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentDeleted(' + path + ')');
                }
                this.onPathDeleted(event, path);
            },

            onContentStateChanged: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentStateChanged(' + path + ')');
                }
                this.refreshTreeNodeState(path);
            },

            setRootPath: function (rootPath, refresh) {
                rootPath = this.adjustRootPath(rootPath);
                if (refresh === undefined) {
                    refresh = (this.rootPath && this.rootPath !== rootPath);
                }
                core.components.Tree.prototype.setRootPath.apply(this, [rootPath, refresh]);
            },

            adjustRootPath: function (rootPath) {
                var scope = pages.getScope();
                if (scope === 'site' && pages.current.site && rootPath.indexOf(pages.current.site) !== 0) {
                    rootPath = pages.current.site;
                }
                return rootPath;
            },

            renameNode: function (node, oldName, newName) {
                var nodePath = node.path;
                var parentPath = core.getParentPath(nodePath);
                var oldPath = core.buildContentPath(parentPath, oldName);
                core.ajaxPost('/bin/cpm/pages/edit.renameContent.json' + core.encodePath(oldPath), {
                    _charset_: 'UTF-8',
                    name: newName
                }, {}, _.bind(function (result) {
                    var reference = result.reference || result.data.reference;
                    if (this.isElementType(node)) {
                        pages.trigger('tree.content.rename', pages.const.event.element.moved,
                            [oldPath, reference.path]);
                    } else {
                        pages.trigger('tree.content.rename', pages.const.event.content.moved,
                            [oldPath, reference.path]);
                    }
                }, this), _.bind(function (result) {
                    this.refreshNodeById(node.id);
                    pages.dialogs.openRenameContentDialog(oldName, oldPath, node.type,
                        _.bind(function (dialog) {
                            dialog.setValues(oldPath, newName);
                            dialog.errorMessage('Rename Content', result);
                        }, this));
                }, this));
            },

            isElementType: function (node) {
                return 'container' === node.type || 'element' === node.type;
            },

            // DnD (page: move/link..., asset: move/reference..., element: move..., component: move/insert...)

            /**
             * the general content DnD move
             */
            dropNode: function (draggedNode, targetNode, index) {
                var targetPath = targetNode.path;
                var oldPath = draggedNode.path;
                var before = this.getNodeOfIndex(targetNode, index);
                // move folders, pages, files...
                var oldParentPath = core.getParentPath(oldPath);
                if (oldParentPath === targetPath) {
                    // reordering in the same parent resource - keep that simple...
                    core.ajaxPost('/bin/cpm/pages/edit.moveContent.json' + core.encodePath(oldPath), {
                        _charset_: 'UTF-8',
                        targetPath: targetPath,
                        before: before ? before.original.path : undefined
                    }, {}, _.bind(function (result) {
                        pages.trigger('tree.content.dnd.drop', pages.const.event.content.moved, [
                            new pages.Reference(draggedNode.name, draggedNode.path, draggedNode.type),
                            new pages.Reference(result.data.reference)
                        ]);
                    }, this), _.bind(function (xhr) {
                        core.alert('error', 'Error', 'Error on moving content', xhr);
                    }, this));
                } else {
                    // move to another parent - check and confirm...
                    pages.dialogs.openMoveContentDialog(draggedNode.name, draggedNode.path, draggedNode.type,
                        _.bind(function (dialog) {
                            dialog.setValues(draggedNode.path, targetNode.path, before ? before.original.name : undefined);
                        }, this));
                }
            },

            nodeIsDraggable: function (selection, event) {
                return true;
            },

            onNodeDragStart: function (event) {
                var node = this.jstree.get_node(event.target);
                if (node) {
                    this.onDragStart(event, {
                        name: node.original.name,
                        path: node.original.path,
                        type: node.original.type
                    });
                    $('#jstree-marker').css('display', 'inherit');
                }
            },

            onDragStart: function (event, reference) {
                var e = pages.const.event;
                if (!reference) {
                    var $el = $(event.currentTarget);
                    reference = new pages.Reference($el);
                }
                this.dnd = undefined;
                if (this.type && reference.path) {
                    var object = {
                        type: this.type,
                        reference: reference
                    };
                    pages.trigger('tree.content.dnd.start', e.dnd.object, [object]);
                    var jsonData = JSON.stringify(object);
                    var dndEvent = event.originalEvent;
                    dndEvent.dataTransfer.setData('application/json', jsonData);
                    dndEvent.dataTransfer.effectAllowed = 'copy';
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        this.log.debug(this.nodeIdPrefix + 'tree.dndStart(' + jsonData + ')');
                    }
                }
            },

            /**
             * adopted from jstree to synchronize the designated move and the tree state in HTML5 DnD mode
             */
            onDragMove: function (event, data) {
                var $target = $(data.event.target).closest('.jstree-node').children('.jstree-anchor');
                if ($target.length === 1 && $target[0] !== data.element) {
                    var offset = $target.offset();
                    var relPos = (data.event.pageY !== undefined
                        ? data.event.pageY : data.event.originalEvent.pageY) - offset.top;
                    var height = $target.outerHeight();
                    var move = 'into';
                    if (relPos < height / 3) {
                        move = 'before';
                    } else if (relPos > height - height / 3) {
                        move = 'after';
                    }
                    this.dnd = {
                        object: data.element,
                        target: this.jstree.get_node(data.event.target, true),
                        move: move
                    };
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        var node = this.jstree.get_node(data.event.target);
                        this.log.debug(this.nodeIdPrefix + 'tree.dndMove('
                            + (node.original ? node.original.path : node.id) + ')');
                    }
                } else {
                    this.dnd = undefined;
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        this.log.debug(this.nodeIdPrefix + 'tree.dndMove(?)');
                    }
                }
            },

            onNodeDragEnd: function (event) {
                if (this.dnd) {
                    var c = tree.const.css;
                    // really inside of the tree?... prevent from move on drag outside of the tree
                    var dnd = core.dnd.getDndData(event);
                    var domEl = document.elementFromPoint(dnd.pos.px, dnd.pos.py);
                    var $tree = $(domEl).closest('.' + c.tools + c._.tree);
                    if ($tree.length === 1) {

                        var object = this.jstree.get_node(this.dnd.object);
                        var target = this.jstree.get_node(this.dnd.target);
                        var index = -1;
                        switch (this.dnd.move) {
                            case 'before':
                                index = this.getNodeIndex(target);
                                break;
                            case 'after':
                                index = this.getNodeIndex(target) + 1;
                                break;
                        }
                        switch (this.dnd.move) {
                            case 'before':
                            case 'after':
                                var parentId = this.jstree.get_parent(target);
                                if (parentId) {
                                    target = this.jstree.get_node(parentId);
                                }
                                break;
                        }
                        if (target.original.path.indexOf(object.original.path) < 0) {
                            var reorder = (target.original.path === core.getParentPath(object.original.path));
                            if (!reorder || index !== this.getNodeIndex(object)) {
                                this.dropNode(object.original, target.original, index, reorder);
                            }
                        }
                    }
                    this.dnd = undefined;
                }
                this.onDragEnd(event);
            },

            onDragEnd: function (event, data) {
                var e = pages.const.event;
                pages.trigger('tree.content.dnd.end', e.dnd.finished, [event], ['...']);
            },

            onDragFinished: function () {
                $.vakata.dnd._clean();
                this.$('.jstree-dnd-parent').removeClass('jstree-dnd-parent');
                $('#jstree-marker').css('display', 'none');
            }
        });

        tree.PageTree = tree.ContentTree.extend({

            nodeIdPrefix: 'PP_',

            initialize: function (options) {
                this.type = 'page';
                var p = pages.const.profile.page.tree;
                var id = this.nodeIdPrefix + 'Tree';
                this.initializeFilter();
                this.suppressEvent = true; // suppress page select events during initialization
                tree.ContentTree.prototype.initialize.apply(this, [options]);
                var e = pages.const.event;
                $(document)
                    .on(e.element.selected + '.' + id, _.bind(this.onElementSelected, this))
                    .on(e.element.inserted + '.' + id, _.bind(this.onElementInserted, this))
                    .on(e.element.changed + '.' + id, _.bind(this.onElementChanged, this))
                    .on(e.element.deleted + '.' + id, _.bind(this.onElementDeleted, this))
                    .on(e.element.moved + '.' + id, _.bind(this.onElementMoved, this))
                    .on(e.content.selected + '.' + id, _.bind(this.onContentSelected, this))
                    .on(e.content.inserted + '.' + id, _.bind(this.onContentInserted, this))
                    .on(e.content.changed + '.' + id, _.bind(this.onContentChanged, this))
                    .on(e.content.deleted + '.' + id, _.bind(this.onContentDeleted, this))
                    .on(e.content.moved + '.' + id, _.bind(this.onContentMoved, this))
                    .on(e.content.state + '.' + id, _.bind(this.onContentStateChanged, this))
                    .on(e.page.selected + '.' + id, _.bind(this.onContentSelected, this))
                    .on(e.page.inserted + '.' + id, _.bind(this.onContentInserted, this))
                    .on(e.page.changed + '.' + id, _.bind(this.onContentChanged, this))
                    .on(e.page.deleted + '.' + id, _.bind(this.onContentDeleted, this))
                    .on(e.site.created + '.' + id, _.bind(this.onContentInserted, this))
                    .on(e.site.changed + '.' + id, _.bind(this.onContentSelected, this))
                    .on(e.site.deleted + '.' + id, _.bind(this.onContentDeleted, this))
                    .on(e.pages.ready + '.' + id, _.bind(this.ready, this));
            },

            /**
             * switch to 'normal'; page select events no longer suppressed
             */
            ready: function () {
                this.suppressEvent = false;
            },

            initializeFilter: function () {
                if (pages.isEditMode()) {
                    var p = pages.const.profile.page.tree;
                    this.filter = pages.profile.get(p.aspect, p.filter, undefined);
                } else {
                    this.filter = undefined;
                }
            },

            dataUrlForPath: function (path) {
                var params = this.filter ? '?filter=' + this.filter : '';
                return '/bin/cpm/pages/edit.pageTree.json' + path + params;
            },

            onNodeSelected: function (path, node) {
                if (!this.suppressEvent) {
                    pages.trigger('tree.page.on.node', "path:select", [path, node.original.name, node.original.type]);
                } else {
                    this.$el.trigger("node:selected", [path]);
                }
            },

            onContentSelected: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                var applicable = /^\/(content)\//.exec(path) !== null;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentSelected(' + applicable + ':' + path + ')');
                }
                if (applicable) {
                    this.onPathSelected(event, path);
                }
            },

            /**
             * returns a reference to the Page of a content element
             * @param elementPath
             * @returns {Window.composum.pages.Reference|undefined|*}
             */
            getPageOfElement: function (elementPath) {
                if (elementPath && elementPath !== '/') {
                    var elementNode = this.getTreeNode(elementPath);
                    if (elementNode) {
                        switch (elementNode.original.type) {
                            case 'pagecontent':
                                var pagePath = core.getParentPath(elementPath);
                                return new pages.Reference(core.getNameFromPath(pagePath),
                                    pagePath, elementNode.original.resourceType, 'cpp:Page');
                            case 'page':
                                return pages.Reference(elementNode.original.name,
                                    elementPath, undefined, elementNode.original.resourceType);
                        }
                    }
                    return this.getPageOfElement(core.getParentPath(elementPath));
                }
                return undefined;
            },

            /**
             * triggers a node state refresh for the page node of the changed content element
             * @param event the element change event
             * @param refOrPath the reference or the path of the changed element
             */
            onPageElementChanged: function (event, refOrPath) {
                var pageRef = this.getPageOfElement(refOrPath && refOrPath.path ? refOrPath.path : refOrPath);
                this.refreshTreeNodeState(pageRef);
            },

            onElementSelected: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onElementSelected(' + path + ')');
                }
                this.onPathSelected(event, path);
            },

            onElementInserted: function (event, parentRefOrPath, /*optional*/ resultRef) {
                var path = parentRefOrPath && parentRefOrPath.path ? parentRefOrPath.path : parentRefOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onElementSelected(' + path + ')');
                }
                this.onPathInserted(event, path, resultRef && resultRef.name ? resultRef.name : '*');
                this.onPageElementChanged(event, path);
            },

            onElementMoved: function (event, oldRefOrPath, newRefOrPath) {
                var oldPath = oldRefOrPath && oldRefOrPath.path ? oldRefOrPath.path : oldRefOrPath;
                var newPath = newRefOrPath && newRefOrPath.path ? newRefOrPath.path : newRefOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onElementSelected(' + oldPath + ' -> ' + newPath + ')');
                }
                this.onPathMoved(event, oldPath, newPath);
                var oldPageRef = this.getPageOfElement(oldPath);
                var newPageRef = this.getPageOfElement(newPath);
                if (oldPageRef) {
                    this.refreshTreeNodeState(oldPageRef);
                }
                if (newPageRef && (!oldPageRef || newPageRef.path !== oldPageRef.path)) {
                    this.refreshTreeNodeState(newPageRef);
                }
            },

            onElementChanged: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onElementChanged(' + path + ')');
                }
                this.onPathChanged(event, path);
                this.onPageElementChanged(event, path);
            },

            onElementDeleted: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onElementDeleted(' + path + ')');
                }
                this.onPathDeleted(event, path);
                this.onPageElementChanged(event, path);
            },

            dropNode: function (draggedNode, targetNode, index) {
                var targetPath = targetNode.path;
                var oldPath = draggedNode.path;
                var before = this.getNodeOfIndex(targetNode, index);
                if (this.isElementType(targetNode) && this.isElementType(draggedNode)) {
                    // move elements on a page or between pages via tree...
                    core.ajaxPost(pages.const.url.edit.move + core.encodePath(oldPath), {
                        _charset_: 'UTF-8',
                        targetPath: targetPath,
                        targetType: targetNode.type,
                        before: before ? before.original.path : undefined
                    }, {}, function (result) {
                        pages.trigger('tree.page.dnd.drop', pages.const.event.element.moved, [
                            new pages.Reference(draggedNode.name, draggedNode.path, draggedNode.type),
                            new pages.Reference(result.reference)
                        ]);
                    }, function (xhr) {
                        core.alert('error', 'Error', 'Error on moving element', xhr);
                    });
                } else {
                    tree.ContentTree.prototype.dropNode.apply(this, [draggedNode, targetNode, index]);
                }
            }
        });

        tree.AssetsTree = tree.ContentTree.extend({

            nodeIdPrefix: 'PA_',

            initialize: function (options) {
                this.type = 'asset';
                this.initializeFilter();
                tree.ContentTree.prototype.initialize.apply(this, [options]);
                var e = pages.const.event;
                var id = this.nodeIdPrefix + 'Tree';
                $(document)
                    .on(e.asset.selected + '.' + id, _.bind(this.onAssetSelected, this))
                    .on(e.content.inserted + '.' + id, _.bind(this.onContentInserted, this))
                    .on(e.content.changed + '.' + id, _.bind(this.onContentChanged, this))
                    .on(e.content.deleted + '.' + id, _.bind(this.onContentDeleted, this))
                    .on(e.content.moved + '.' + id, _.bind(this.onContentMoved, this))
                    .on(e.content.state + '.' + id, _.bind(this.onContentStateChanged, this))
                    .on(e.content.view + '.' + id, _.bind(this.onAssetSelected, this));
            },

            initializeFilter: function () {
                var p = pages.const.profile.asset.tree;
                this.filter = pages.profile.get(p.aspect, p.filter, undefined);
            },

            dataUrlForPath: function (path) {
                var params = this.filter ? '?filter=' + this.filter : '';
                return '/bin/cpm/pages/assets.assetTree.json' + path + params;
            },

            onNodeSelected: function (path, node) {
                if (!this.suppressEvent) {
                    pages.trigger('tree.asset.on.node', "asset:select", [path, node.original.name, node.original.type]);
                } else {
                    core.components.Tree.prototype.onNodeSelected.apply(this, [path, node]);
                }
            },

            onAssetSelected: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                var applicable = /^\/(content)\//.exec(path) !== null;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onAssetSelected(' + applicable + ':' + path + ')');
                }
                if (applicable) {
                    this.onPathSelected(event, path);
                    var node = this.getTreeNode(path);
                    if (node && node.original.type !== 'folder') {
                        if (node.original.type !== 'site') {
                            var p = pages.const.profile.asset.tree;
                            pages.profile.set(p.aspect, p.path, path);
                            pages.current.file = path;
                        } else {
                            pages.trigger('tree.asset.on.node', "path:select", [path, node.original.name, node.original.type]);
                        }
                    }
                }
            }
        });

        tree.DevelopTree = tree.ContentTree.extend({

            nodeIdPrefix: 'PD_',

            initialize: function (options) {
                tree.ContentTree.prototype.initialize.apply(this, [options]);
                var e = pages.const.event;
                var id = this.nodeIdPrefix + 'Tree';
                $(document)
                    .on(e.content.selected + '.' + id, _.bind(this.onContentSelected, this))
                    .on(e.folder.inserted + '.' + id, _.bind(this.onFolderInserted, this))
                    .on(e.content.inserted + '.' + id, _.bind(this.onContentInserted, this))
                    .on(e.content.changed + '.' + id, _.bind(this.onContentChanged, this))
                    .on(e.content.deleted + '.' + id, _.bind(this.onContentDeleted, this))
                    .on(e.content.moved + '.' + id, _.bind(this.onContentMoved, this));
            },

            dataUrlForPath: function (path) {
                return '/bin/cpm/pages/develop.tree.json' + path;
            },

            adjustRootPath: function (rootPath) {
                return rootPath;
            },

            onNodeSelected: function (path, node) {
                if (!this.suppressEvent) {
                    pages.trigger('tree.develop.on.node', "content:select", [path, node.original.name, node.original.type]);
                } else {
                    core.components.Tree.prototype.onNodeSelected.apply(this, [path, node]);
                }
            },

            onContentSelected: function (event, refOrPath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                var applicable = /^\/(apps|libs)\//.exec(path) !== null;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onContentSelected(' + applicable + ':' + path + ')');
                }
                if (applicable) {
                    this.onPathSelected(event, path);
                    var p = pages.const.profile.component.tree;
                    pages.profile.set(p.aspect, p.path, path);
                }
            },

            onFolderInserted: function (event, refOrPath, relativePath) {
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onFolderInserted(' + path + ',' + relativePath + ')');
                }
                this.onContentInserted(event, refOrPath);
                window.setTimeout(_.bind(function () {
                    this.onContentSelected(event, path + '/' + relativePath);
                }, this), 100);
            }
        });

        tree.ToolsTreePanel = Backbone.View.extend({

            initialize: function (options) {
                this.$actions = this.$('.' + tree.const.actions.css);
                this.treePanelId = this.tree.nodeIdPrefix + 'treePanel';
            },

            onNodeSelected: function (event, path) {
                if (this.path !== path) {
                    if (pages.log.getLevel() <= log.levels.DEBUG) {
                        pages.log.debug(this.treePanelId + '.onNodeSelected(' + path + ')');
                    }
                    this.path = path;
                    if (!path) {
                        this.doSelectDefaultNode();
                    } else {
                        this.setNodeActions();
                    }
                }
            },

            doSelectDefaultNode: function () {
                var busy = this.tree.busy;
                this.tree.busy = true;
                this.selectDefaultNode();
                this.tree.busy = busy;
            },

            getNodeActionsUrl: function (path) {
                return tree.const.actions.url + core.encodePath(path);
            },

            setNodeActions: function () {
                if (this.actions) {
                    this.actions.dispose();
                    this.actions = undefined;
                }
                var node = this.tree.getSelectedTreeNode();
                if (node.original.path) {
                    if (this.$actions.length > 0) {
                        // load tree actions for the selected resource (if actions are used in the tree)
                        core.ajaxGet(this.getNodeActionsUrl(node.original.path), {},
                            _.bind(function (data) {
                                this.$actions.html(data);
                                this.actions = core.getWidget(this.$actions[0],
                                    '.' + pages.toolbars.const.editToolbarClass, pages.toolbars.EditToolbar);
                                this.actions.data = {
                                    path: node.original.path
                                };
                            }, this));
                    }
                }
                this.showPreview(node);
            },

            showPreview: function (node) {
            }
        });

        tree.ContentTreePanel = tree.ToolsTreePanel.extend({

            initialize: function (options) {
                var c = tree.const.css;
                tree.ToolsTreePanel.prototype.initialize.apply(this, [options]);
                this.$treePanel = this.$('.' + c.tools + c._.treePanel);
                this.$treePanelPreview = this.$('.' + c.preview);
                this.searchPanel = core.getWidget(this.el, '.' + pages.search.const.css.base + pages.search.const.css._panel,
                    pages.search.SearchPanel);
            },

            refresh: function () {
                if (this.currentView === 'search') {
                    this.searchPanel.refresh();
                } else {
                    this.tree.refresh();
                }
            },

            onScopeChanged: function () {
                if (this.tree.log.getLevel() <= log.levels.DEBUG) {
                    this.tree.log.debug(this.tree.nodeIdPrefix + 'treePanel.onScopeChanged()');
                }
                this.tree.setRootPath('/');
                this.searchPanel.onScopeChanged();
            },

            onSiteSelected: function () {
                if (this.tree.log.getLevel() <= log.levels.DEBUG) {
                    this.tree.log.debug(this.tree.nodeIdPrefix + 'treePanel.onSiteSelected()');
                }
                this.onScopeChanged();
            },

            onTabSelected: function () {
                if (this.tree.log.getLevel() <= log.levels.DEBUG) {
                    this.tree.log.debug(this.tree.nodeIdPrefix + 'treePanel.onTabSelected()');
                }
                if (this.currentView === 'search') {
                    this.searchPanel.onShown();
                }
            },

            setView: function (key) {
                if (!key) {
                    key = !this.currentView || this.currentView === 'search' ? 'tree' : 'search';
                }
                if (this.currentView !== key) {
                    this.currentView = key;
                    var p = pages.const.profile[this.type].tree;
                    pages.profile.set(p.aspect, p.view, key);
                    switch (key) {
                        default:
                        case 'tree':
                            this.$viewToggle.removeClass('fa-sitemap').addClass('fa-search');
                            this.searchPanel.$el.addClass('hidden');
                            this.$treePanel.removeClass('hidden');
                            this.$treePanelPreview.removeClass('hidden');
                            this.searchPanel.onHidden();
                            break;
                        case 'search':
                            this.$viewToggle.removeClass('fa-search').addClass('fa-sitemap');
                            this.$treePanelPreview.addClass('hidden');
                            this.$treePanel.addClass('hidden');
                            this.searchPanel.$el.removeClass('hidden');
                            this.searchPanel.onShown();
                            break;
                    }
                }
            },

            toggleView: function (event) {
                event.preventDefault();
                this.setView();
                return false;
            },

            selectFilter: function (event) {
                event.preventDefault();
                var p = pages.const.profile[this.type].tree;
                var $link = $(event.currentTarget);
                var filter = $link.parent().data('value');
                this.tree.setFilter(filter);
                this.searchPanel.setFilter(filter);
                this.setFilter(filter);
                pages.profile.set(p.aspect, p.filter, filter);
            }
        });

        tree.PageTreePanel = tree.ContentTreePanel.extend({

            initialize: function (options) {
                this.type = 'page';
                var c = tree.const.css;
                var e = pages.const.event;
                var p = pages.const.profile[this.type].tree;
                this.tree = core.getWidget(this.el, '.' + c.tools + c._.tree, tree.PageTree);
                this.tree.panel = this;
                tree.ContentTreePanel.prototype.initialize.apply(this, [options]);
                this.$viewToggle = this.$('.' + tree.const.pages.css.base + '_toggle-view');
                this.$viewToggle.click(_.bind(this.toggleView, this));
                this.setView(pages.profile.get(p.aspect, p.view, 'tree'));
                this.setFilter(pages.profile.get(p.aspect, p.filter, undefined));
                this.$('.' + tree.const.pages.css.base + '_filter-value a').click(_.bind(this.selectFilter, this));
                this.$('.' + c.main + c._.pages + c._.refresh).click(_.bind(this.refresh, this));
                this.tree.$el.on('node:selected.' + this.treePanelId, _.bind(this.onNodeSelected, this));
                $(document)
                    .on(e.site.selected + '.' + this.treePanelId, _.bind(this.onSiteSelected, this))
                    .on(e.scope.changed + '.' + this.treePanelId, _.bind(this.onScopeChanged, this));
            },

            selectDefaultNode: function () {
                if (pages.current.page) {
                    this.tree.selectNode(pages.current.page, _.bind(function () {
                        this.setNodeActions();
                    }, this));
                }
            },

            setFilter: function (filter) {
                this.$('.' + tree.const.pages.css.base + '_filter-value').removeClass('active');
                if (filter) {
                    this.$('.' + tree.const.pages.css.base + '_filter-value[data-value="' + filter + '"]')
                        .addClass('active');
                }
            }
        });

        tree.AssetsTreePanel = tree.ContentTreePanel.extend({

            initialize: function (options) {
                this.type = 'asset';
                var c = tree.const.css;
                var e = pages.const.event;
                var p = pages.const.profile[this.type].tree;
                this.tree = core.getWidget(this.el, '.' + c.tools + c._.tree, tree.AssetsTree);
                this.tree.panel = this;
                tree.ContentTreePanel.prototype.initialize.apply(this, [options]);
                this.$viewToggle = this.$('.' + tree.const.assets.css.base + '_toggle-view');
                this.$viewToggle.click(_.bind(this.toggleView, this));
                this.setView(pages.profile.get(p.aspect, p.view, 'tree'));
                this.setFilter(pages.profile.get(p.aspect, p.filter, undefined));
                this.$('.' + tree.const.assets.css.base + '_filter-value a').click(_.bind(this.selectFilter, this));
                this.$('.' + c.main + c._.assets + c._.refresh).click(_.bind(this.refresh, this));
                this.tree.$el.on('node:selected.' + this.treePanelId, _.bind(this.onNodeSelected, this));
                $(document)
                    .on(e.asset.select + '.' + this.treePanelId, _.bind(this.selectAsset, this))
                    .on(e.site.selected + '.' + this.treePanelId, _.bind(this.onSiteSelected, this))
                    .on(e.scope.changed + '.' + this.treePanelId, _.bind(this.onScopeChanged, this))
                    .on(e.pages.ready + '.' + this.treePanelId, _.bind(this.onReady, this));
            },

            onReady: function (event) {
                var p = pages.const.profile.asset.tree;
                var path = pages.profile.get(p.aspect, p.path, '');
                if (path) {
                    this.tree.selectNode(path);
                }
            },

            selectAsset: function (event, refOrPath) {
                var e = pages.const.event;
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                pages.trigger('tree.asset.select', e.asset.selected, [path]);
            },

            selectDefaultNode: function () {
            },

            getNodeActionsUrl: function (path) {
                return tree.const.actions.assets + path;
            },

            setFilter: function (filter) {
                var c = tree.const.assets.css;
                this.$('.' + c.base + '_filter-value').removeClass('active');
                if (filter) {
                    this.$('.' + c.base + '_filter-value[data-value="' + filter + '"]')
                        .addClass('active');
                }
            },

            showPreview: function (node) {
                if (node && node.original.path && node.original.type.indexOf('file') >= 0) {
                    core.ajaxGet(tree.const.tile.asset + core.encodePath(node.original.path), {},
                        _.bind(function (data) {
                            if (data) {
                                this.$treePanelPreview.html(data);
                                this.$el.addClass('preview-available');
                                core.getWidget(this.$el, '.' + pages.tools.const.fileStatus.css.base, pages.tools.FileStatus);
                                this.$treePanelPreview.find('[draggable="true"]')
                                    .on('dragstart', _.bind(this.onPreviewDragStart, this))
                                    .on('dragend', _.bind(this.tree.onDragEnd, this.tree));
                            } else {
                                this.$el.removeClass('preview-available');
                                this.$treePanelPreview.html('');
                            }
                        }, this));
                } else {
                    this.$el.removeClass('preview-available');
                    this.$treePanelPreview.html('');
                }
            },

            onPreviewDragStart: function (event) {
                var $el = $(event.currentTarget);
                var reference = new pages.Reference($el);
                if (reference.path) {
                    this.tree.onDragStart(event, reference);
                }
            }
        });

        tree.DevelopTreePanel = tree.ContentTreePanel.extend({

            initialize: function (options) {
                this.type = 'component';
                var c = tree.const.css;
                var e = pages.const.event;
                var p = pages.const.profile[this.type].tree;
                this.tree = core.getWidget(this.el, '.' + c.tools + c._.tree, tree.DevelopTree);
                this.tree.panel = this;
                tree.ContentTreePanel.prototype.initialize.apply(this, [options]);
                this.$viewToggle = this.$('.' + tree.const.develop.css.base + '_toggle-view');
                this.$viewToggle.click(_.bind(this.toggleView, this));
                this.setView(pages.profile.get(p.aspect, p.view, 'tree'));
                this.setFilter(pages.profile.get(p.aspect, p.filter, undefined));
                this.$('.' + tree.const.develop.css.base + '_filter-value a').click(_.bind(this.selectFilter, this));
                this.$('.' + c.main + c._.develop + c._.refresh).click(_.bind(this.refresh, this));
                this.tree.$el.on('node:selected.' + this.treePanelId, _.bind(this.onNodeSelected, this));
                $(document)
                    .on(e.content.select + '.' + this.treePanelId, _.bind(this.selectContent, this))
                    .on(e.site.selected + '.' + this.treePanelId, _.bind(this.onSiteSelected, this))
                    .on(e.scope.changed + '.' + this.treePanelId, _.bind(this.onScopeChanged, this))
                    .on(e.pages.ready + '.' + this.treePanelId, _.bind(this.onReady, this));
            },

            onReady: function (event) {
                var p = pages.const.profile.component.tree;
                var path = pages.profile.get(p.aspect, p.path, '');
                if (path) {
                    this.selectContent(event, path);
                }
            },

            selectContent: function (event, refOrPath) {
                var e = pages.const.event;
                var path = refOrPath && refOrPath.path ? refOrPath.path : refOrPath;
                pages.trigger('tree.develop.select', e.content.selected, [path]);
            },

            selectDefaultNode: function () {
            },

            getNodeActionsUrl: function (path) {
                return tree.const.actions.develop + path;
            },

            setFilter: function (filter) {
                var c = tree.const.develop.css;
                this.$('.' + c.base + '_filter-value').removeClass('active');
                if (filter) {
                    this.$('.' + c.base + '_filter-value[data-value="' + filter + '"]')
                        .addClass('active');
                }
            }
        });

        tree.setup = function () {
            tree.pageTreePanel = core.getView('.' + tree.const.pages.css.base, tree.PageTreePanel);
            tree.assetsTreePanel = core.getView('.' + tree.const.assets.css.base, tree.AssetsTreePanel);
            tree.developTreePanel = core.getView('.' + tree.const.develop.css.base, tree.DevelopTreePanel);
        };
        tree.setup();
        $(document).on(pages.const.event.pages.ready + '.Tree', function () {
            tree.setup();
        });

    })(window.composum.pages.tree, window.composum.pages, window.core);
})(window);
