(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('DetailsController', DetailsController);

    DetailsController.$inject = ['$scope', '$state', 'dataService', '$log', 'utils', '$uibModal'];

    function DetailsController ($scope, $state, dataService, $log, utils, $uibModal) {
        var vm = this;
        var processInstanceId = $state.params.processInstanceId;
        $scope.processInstanceId = $state.params.processInstanceId;
        vm.data = {};

        if (processInstanceId) {
            $log.info("getting task info");

            vm.data.processInstanceId = processInstanceId;
            dataService.processInstances.byProcessInstanceId(processInstanceId).then(
                    function(response) {
                        vm.data.entity = utils.refactoringVariables([response.data.entity])[0];
                        vm.data.history = response.data.history;
                        vm.data.attachments = response.data.attachments;
                        vm.diagramUrl = '/rest/diagram/processInstance/'+ vm.data.entity.id;
                    });

        }
    }
})();
