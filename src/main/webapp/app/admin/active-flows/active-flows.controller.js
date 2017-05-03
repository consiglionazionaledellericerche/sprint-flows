(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('ActiveFlowsController', ActiveFlowsController);

    ActiveFlowsController.$inject = ['$scope', 'dataService', '$log'];

    function ActiveFlowsController ($scope, dataService, $log) {
        var vm = this;
        $scope.setActiveContent = function(choice) {
            vm.activeContent = choice;
        }

        dataService.processInstances.getProcessInstance(true)
        .then(function (response) {
            vm.activeInstances = response;
        }, function (response) {
            $log.error(response);
        });
        dataService.processInstances.getProcessInstance(false)
        .then(function (response) {
            vm.completedInstances = response;
        }, function (response) {
            $log.error(response);
        });
    }

})();
