(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('textlookup', textlookup);

    textlookup.$inject = ['dataService', '$log', 'utils'];

    function textlookup(dataService, $log, utils) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/textlookup/textlookup.html',
            scope: {
                ngModel: '=',
                type: '@',
                id: '@',
                ngReadonly: '@'
            },
            link: function ($scope, element, attrs) {

                dataService.lookup[$scope.type]().then(function(response) {
                    $scope.value = response.data.value;
                })

            }
        }
    }
})();