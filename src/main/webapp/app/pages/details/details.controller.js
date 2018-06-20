(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('DetailsController', DetailsController);

    DetailsController.$inject = ['$scope', '$state', 'dataService', '$log', 'utils', '$uibModal', '$window'];

    function DetailsController($scope, $state, dataService, $log, utils, $uibModal, $window) {
        var vm = this;
        vm.data = {};
        vm.taskId = $state.params.taskId;
        $scope.processInstanceId = $state.params.processInstanceId; // mi torna comodo per gli attachments -martin

        if ($state.params.processInstanceId) {
            dataService.processInstances.byProcessInstanceId($state.params.processInstanceId, true).then(
                function(response) {
                    vm.data.entity = utils.refactoringVariables([response.data.entity])[0];
                    vm.data.history = response.data.history;
                    //in response.data.entity.variables ci sono anche le properties della Process Instance (initiator, startdate, ecc.)
                    vm.data.startEvent = response.data.entity.variables;
                    vm.data.attachments = utils.parseAttachments(response.data.attachments);
                    vm.data.identityLinks = response.data.identityLinks;
                    vm.diagramUrl = '/rest/diagram/processInstance/' + vm.data.entity.id + "?" + new Date().getTime();

                    vm.links = [];
                    response.data.entity.variabili.linkToOtherWorkflows.split(',').forEach(function(processInstanceId) {
                        dataService.processInstances.getVariable(processInstanceId, 'titolo').success(function(titolo) {
                            vm.links.push({
                                titolo: titolo.value,
                                processInstanceId: processInstanceId
                            });
                        });
                    });

                    var processDefinitionKey = response.data.entity.processDefinitionId.split(":")[0];
                    vm.detailsView = 'api/views/' + processDefinitionKey + '/detail';
                    vm.experiences = jQuery.parseJSON(vm.data.entity.variabili.valutazioneEsperienze_json);
                    vm.data.history.forEach(function(el) {
                        //recupero l'ultimo task (quello ancora da eseguire)
                        if (el.historyTask.endTime === null) {
                            //recupero la fase
                            vm.data.fase = el.historyTask.name;
                            //recupero il gruppo/l'utente assegnatario del task
                            el.historyIdentityLink.forEach(function(il) {
                                if (il.type === "candidate")
                                    if (il.groupId !== null)
                                        vm.data.groupCandidate = il.groupId;
                                    else
                                        vm.data.userCandidate = il.userId
                            })
                        }
                    });
                });
        }


        $scope.exportSummary = function(url, filename) {
            utils.downloadFile(url, filename, 'application/pdf');
        };


        $scope.editVariable = function(variableName, currentValue) {
            $uibModal.open({
                templateUrl: 'app/pages/details/editvariable.modal.html',
                controller: 'EditVariableModalController',
                controllerAs: 'vm',
                size: 'md',
                resolve: {
                    processInstanceId: function() {
                        return $scope.processInstanceId;
                    },
                    variableName: function() {
                        return variableName;
                    },
                    currentValue: function() {
                        return currentValue;
                    }

                }
            })
        };


        $scope.history = function(tasks, startTask) {
            $uibModal.open({
                templateUrl: 'app/pages/details/history.modal.html',
                controller: 'HistoryModalController',
                controllerAs: 'vm',
                size: 'md',
                resolve: {
                    tasks: function() {
                        return tasks;
                    },
                    startTask: function() {
                        return startTask;
                    }
                }
            })
        };
    }
})();