angular.module('ngGridster', []);

angular.module('ngGridster').directive('gridster', [
    function () {
        return {
            restrict: 'E',
            template: '<div class="gridster"><div ng-transclude/></div>',
            transclude: true,
            replace: true,
            controller: function () {
                gr = null;
                return {
                    init: function (elem, options) {
                        ul = $(elem);
                        gr = ul.gridster(angular.extend({
                            widget_selector: 'gridster-item',
                        	widget_selector: 'li'	
                        }, options)).data('gridster');
                    },
                    addItem: function (elm)  {
                        gr.add_widget(elm);
                    },
                    removeItem: function (elm) {
                        gr.remove_widget(elm);
                    }
                };
            },
            link: function (scope, elem, attrs, controller) {
              var options = scope.$eval(attrs.options);
              controller.init(elem, options);
            }
        };
    }
]);

angular.module('ngGridster').directive('gridsterItem', [
    function () {
        return {
            restrict: 'EA',
            require: '^gridster',
            link: function (scope, elm, attrs, controller) {
                controller.addItem(elm);
                elm.bind('$destroy', function () {
                    controller.removeItem(elm);
                });
            }
        };
    }
]);