(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('cnrTree', cnrTree);

    cnrTree.$inject = ['dataService', '$sessionStorage', '$log'];

    function cnrTree(dataService, $sessionStorage, $log) {
        return {
            restrict: 'E',
            scope: {
                listName: '@',
                cdsuo: '@?',
                label: '@',
                ngModel: '='
            },
            templateUrl: 'app/components/jstree/jstree.html',


            link: function link(scope) {

                scope.treeConfig = {
                        version: 1
                };
                scope.jsonlist = [];
                scope.ignoreModelChanges = function() {return true;};

                dataService.dynamiclist.byName(scope.listName).then(
                        function(response) {
                            var lists = JSON.parse(response.data.listjson);
                            var type = (scope.cdsuo !== undefined && scope.cdsuo !== '') ? scope.cdsuo : 'default';
                            scope.jsonlist = lists[type];
                            $log.info(scope.jsonlist);
                            scope.treeConfig.version++;
                        }, function(response) {
                            $log.error(response);
                        }
                );

                scope.select_node = function (discard, selection) {
                    if (selection.node.children.length === 0) {
                        scope.ngModel = selection.node.text;
                    } else {
                        scope.treeInstance.jstree(true).deselect_all({});
                    }
                };
            }

        };
    }
})();