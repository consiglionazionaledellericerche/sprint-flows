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

                $scope.loadRecords = function(filter) {
                    switch ($scope.type) {
                        case 'users':
                            return dataService.search.users(filter)
                                .then(function(response) {
                                    $scope.hasMore = response.data.more;
                                    return response.data.results;
                                });
                            break;
                        case 'uo':
                            return dataService.search.uo(filter)
                                .then(function(response) {
                                    $scope.hasMore = response.data.more;
                                    return response.data.results;
                                });
                        case 'flowsUsers':
                            return dataService.search.flowsUsers(filter)
                                .then(function(response) {
                                    $scope.hasMore = response.data.more;
                                    return response.data.results;
                                });
                            break;
                        case 'flowsGroups':
                            return dataService.search.flowsGroups(filter)
                                .then(function(response) {
                                    $scope.hasMore = response.data.more;
                                    return response.data.results;
                                });
                            break;
                        default:
                            $log.error('Type non riconosciuto ' + $scope.type);
                    }
                };
            },
        };
    }
})();
