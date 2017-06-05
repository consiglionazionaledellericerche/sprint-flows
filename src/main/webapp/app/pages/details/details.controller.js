(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('DetailsController', DetailsController);

    DetailsController.$inject = ['$scope', '$state', 'dataService', '$log', 'utils', '$uibModal', '$window', 'Lightbox'];

    function DetailsController ($scope, $state, dataService, $log, utils, $uibModal, $window, Lightbox) {
        var vm = this;
        vm.data = {};
        vm.taskId = $state.params.taskId;
        $scope.processInstanceId = $state.params.processInstanceId; // mi torna comodo per gli attachments -martin

        if ($state.params.processInstanceId) {
            dataService.processInstances.byProcessInstanceId($state.params.processInstanceId).then(
                    function(response) {
                        vm.data.entity = utils.refactoringVariables([response.data.entity])[0];
                        vm.data.history = response.data.history;
                        //in response.data.entity.variables ci sono anche le properties della Process Instance (initiator, startdate, ecc.)
                        vm.data.startEvent = response.data.entity.variables;
                        vm.data.attachments = response.data.attachments;
                        vm.data.identityLinks = response.data.identityLinks;
                        vm.diagramUrl = '/rest/diagram/processInstance/'+ vm.data.entity.id;

                        var processDefinitionId = response.data.entity.processDefinitionId.split(":")[0];
                        vm.detailsView = 'api/views/'+ processDefinitionId +'/detail';

                    });
        }

        $scope.openDiagramModal = function() {
            Lightbox.openModal(['/rest/diagram/processInstance/'+vm.data.entity.id], 0);
        }
    }
})();
