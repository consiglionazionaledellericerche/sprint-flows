(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('typeaheadinput', typeaheadinput);

    typeaheadinput.$inject = ['dataService', '$log'];

    /**
     * Questa direttiva e' un semplicissimo typeahead con dei parametri preimpostati
     * L'API e' semplicissima: ng-model e ng-required
     */
    function typeaheadinput(dataService, $log) {
        return {
            restrict: 'E',
            templateUrl: 'app/inputs/typeaheadinput/typeaheadinput.html',
            scope: {
                ngModel: '=',
                labelModel: '=',
                type: '@',
                ngRequired: '@',
            },
            link: function($scope, element, attrs) {
                $scope.localModel = undefined;

                $scope.$watch('localModel', function(newVal) {
                    if (newVal) {
                        $scope.ngModel = newVal.value;
                        $scope.labelModel = newVal.label;
                    }
                });

                init();

                $scope.loadRecords = function(filter) {
                    return dataService.search[$scope.type](filter)
                       .then(function(response) {
                           $scope.hasMore = response.data.more;
                           return response.data.results;
                       });
                };

                function init() {

                    if ('autofill' in attrs) {
                        var nomeModelId = attrs.ngModel.split('.').pop();
                        $scope.ngModel = $scope.$parent.data.entity.variabili[nomeModelId];
                    }

                    if ($scope.ngModel) {

                        dataService.lookup[$scope.type]($scope.ngModel)
                            .then(function (response) {
                                $scope.localModel = response.data;
                            });
                    }
                }
            }
        };
    }
})();
