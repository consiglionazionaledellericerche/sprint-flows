(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('fileinput', fileinput);

    fileinput.$inject = ['dataService', '$log'];

    function fileinput(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/fileinput/fileinput.html',
            scope: true,
            link: function ($scope, element, attrs) {
                $scope.attrs = attrs;
                $scope.model = attrs.model;
            }
        }
    }
})();