(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('ActiveFlowsController', ActiveFlowsController);

    ActiveFlowsController.$inject = ['$scope', 'dataService', '$log', 'utils'];

    function ActiveFlowsController ($scope, dataService, $log, utils) {
        var vm = this;
        $scope.setActiveContent = function(choice) {
            vm.activeContent = choice;
        }

        dataService.processInstances.getActive()
        .then(function (response) {
            vm.activeInstances = response;
        }, function (response) {
            $log.error(response);
        });
        dataService.processInstances.getCompleted()
        .then(function (response) {
            vm.completedInstances = response;
        }, function (response) {
            $log.error(response);
        });
    }

})();
