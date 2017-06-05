(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('subform', subform);

    subform.$inject = ['dataService', '$log'];

    function subform(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/subform/subform.html',
            scope: {
                ngModel: '=',
                label: '@',
                multiple: '@',
                subformName: '@'
            },
            link: function ($scope, element, attrs) {
                var subform = this;
                $scope.subform = subform;

                subform.formUrl = 'api/forms/'+ $scope.$parent.vm.processDefinitionKey +"/"+$scope.$parent.vm.processVersion +"/"+ $scope.subformName;

                $scope.addRow = function() {
                    $scope.ngModel.push({});
                    return false;
                };
                $scope.removeRow = function() {
                    $scope.ngModel.pop();
                    return false;
                };
            }
        }
    }
})();