(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('metadatum', metadatum);

    metadatum.$inject = ['dataService', '$log'];

    function metadatum(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/metadatum/metadatum.html',
            scope: {
                label: '@',
                value: '@',
                type: '@?'
            },
            link: function ($scope, element, attrs) {

                $scope.type = $scope.type | "text";
                $scope.attrs = attrs;
            }
        }


    }
})();