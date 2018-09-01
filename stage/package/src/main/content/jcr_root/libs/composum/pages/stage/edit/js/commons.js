(function (window) {
    window.composum = window.composum || {};
    window.composum.pages = window.composum.pages || {};
    (function (pages, core) {
        'use strict';

        pages.const = _.extend(pages.const || {}, {
            commons: {
                data: { // the data attribute names of a component
                    reference: 'pages-edit-reference',
                    name: 'pages-edit-name',
                    path: 'pages-edit-path',
                    type: 'pages-edit-type',
                    prim: 'pages-edit-prim',
                    synthetic: 'pages-edit-synthetic'
                },
                url: {
                    edit: '/bin/cpm/pages/edit',
                    _resourceInfo: '.resourceInfo.json'
                }
            }
        });

        pages.Reference = function (nameOrView, path, type, prim, synthetic) {
            if (nameOrView instanceof Backbone.View) {
                nameOrView = nameOrView.$el;
            }
            if (nameOrView instanceof jQuery) {
                var d = pages.const.commons.data;
                var reference = nameOrView.data(d.reference);
                if (reference) {
                    _.extend(this, reference);
                } else {
                    this.name = nameOrView.data(d.name);
                    this.path = path || nameOrView.data(d.path);
                    this.type = type || nameOrView.data(d.type);
                    this.prim = prim || nameOrView.data(d.prim);
                    this.synthetic = synthetic !== undefined ? synthetic
                        : core.parseBool(nameOrView.data(d.synthetic));
                }
            } else {
                this.name = nameOrView; // resource name
                this.path = path;       // resource path
                this.type = type;       // resource type
                this.prim = prim;       // primary / component type
                this.synthetic = synthetic !== undefined && synthetic;
            }
        };
        _.extend(pages.Reference.prototype, {

            /**
             * check reference data on completeness
             */
            isComplete: function () {
                return this.name !== undefined && this.path !== undefined &&
                    this.type !== undefined && this.prim !== undefined;
            },

            /**
             * load reference information if necessary and call callback after data load if a callback is present
             */
            complete: function (callback) {
                if (!this.isComplete()) {
                    var u = pages.const.commons.url;
                    var options = {};
                    if (this.type) {
                        options.data = {
                            type: this.type
                        };
                    }
                    core.ajaxGet(u.edit + u._resourceInfo + this.path, options, _.bind(function (data) {
                        // '' as fallback to prevent from infinite recursion..
                        if (!this.name) {
                            this.name = data.name ? data.name : '';
                        }
                        if (!this.type) {
                            this.type = data.type ? data.type : '';
                        }
                        if (!this.prim) {
                            this.prim = data.prim ? data.prim : '';
                        }
                        this.synthetic = data.synthetic;
                        if (_.isFunction(callback)) {
                            callback(this);
                        }
                    }, this));
                } else {
                    if (_.isFunction(callback)) {
                        callback(this);
                    }
                }
            }
        });

    })(window.composum.pages, window.core);
})(window);
