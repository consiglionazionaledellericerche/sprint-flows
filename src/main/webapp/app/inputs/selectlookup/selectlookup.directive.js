(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('selectlookup', selectlookup);

    selectlookup.$inject = ['dataService', '$log', 'utils'];

    function selectlookup(dataService, $log, utils) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/selectlookup/selectlookup.html',
            scope: {
                ngModel: '=',
                type: '@',
                id: '@'
            },
            link: function ($scope, element, attrs) {

                dataService.lookup[$scope.type]().then(function(response) {
                    $scope.records = response.data;
                })

            }
        }
    }
})();