(function () {
    'use strict';
    CPM.namespace('pages.components');

    (function (components, pages, core) {
        'use strict';

        components.const = _.extend(components.const || {}, {
            illustration: {
                cssBase: 'composum-pages-components-composed-illustration',
                _shape: '-annotation_shape',
                _link: '-annotation_link'
            }
        });

        components.Illustration = Backbone.View.extend({

            initialize: function (options) {
                var c = components.const.illustration;
                var self = this;
                this.$('.' + c.cssBase + c._link).popover({
                    html: true,
                    sanitize: false,
                    trigger: this.$el.data('behavior') === 'independent' ? 'click' : 'focus',
                    viewport: function () {
                        return self.el;
                    }
                });
            }
        });

        $(document).ready(function () {
            $('.' + components.const.illustration.cssBase).each(function () {
                core.getView(this, components.Illustration);
            });
        });

    })(CPM.pages.components, CPM.pages, CPM.core);
})();
