(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('DetailsController', DetailsController);

    DetailsController.$inject = ['$scope', '$rootScope', 'Principal', '$state', '$localStorage', 'dataService', '$log', 'utils', '$uibModal'];

    function DetailsController($scope, $rootScope, Principal, $state, $localStorage, dataService, $log, utils, $uibModal) {
        var vm = this;
        vm.searchParams = {};
        vm.data = {};
        vm.taskId = $state.params.taskId;
        //vm.searchParams.statoFinaleDomanda = {};        
        vm.showGerarchia = false;
        vm.searchParams.active = true;
        vm.searchParams.order = "ASC";
        vm.searchParams.page = 1;
        vm.searchParams.processDefinitionKey = "short-term-mobility-domande";
        vm.searchParams.statoFinaleDomanda = "text=VALUTATA_SCIENTIFICAMENTE";


        $scope.processInstanceId = $state.params.processInstanceId; // mi torna comodo per gli attachments -martin

        Principal.identity().then(function(account) {
            vm.username    = account.login;
            vm.authorities = account.authorities;
        });

        if ($state.params.processInstanceId) {
            dataService.processInstances.byProcessInstanceId($state.params.processInstanceId, true).then(
                function(response) {
                    vm.data.entity = utils.refactoringVariables([response.data.entity])[0];
                    vm.initiator = JSON.parse(vm.data.entity.name).initiator; //serve per richiamare la "cronologia"
                    vm.data.linkedProcesses = response.data.linkedProcesses;
                    vm.data.history = response.data.history;
                    //in response.data.entity.variables ci sono anche le properties della Process Instance (initiator, startdate, ecc.)
                    vm.data.startEvent = response.data.entity.variables;
                    vm.data.attachments = utils.parseAttachments(response.data.attachments);
                    vm.data.identityLinks = response.data.identityLinks;
                    vm.diagramUrl = '/rest/diagram/processInstance/' + vm.data.entity.id + "?" + new Date().getTime();

                    var processDefinition = response.data.entity.processDefinitionId.split(":");
                    var stato = response.data.history[0].historyTask.name;

                    vm.detailsView = 'api/views/' + processDefinition[0] + '/' + processDefinition[1] + '/detail';

                    if(vm.data.entity.variabili.valutazioneEsperienze_json){
                        vm.experiences = jQuery.parseJSON(vm.data.entity.variabili.valutazioneEsperienze_json);
                    }
                    if(processDefinition[0] != null & processDefinition[0] == "short-term-mobility-bando-dipartimento" & stato == "PROVVEDIMENTO GRADUATORIA"
) {
                    		vm.showGerarchia = true;
                	}
                    vm.searchParams.idBando = "text="+response.data.variabili.idBando.value;
                    vm.searchParams.dipartimentoId = "text="+response.data.variabili.dipartimentoId.value;
                    
                    
                    
                    vm.data.history.forEach(function(el) {
                        //recupero l'ultimo task (quello ancora da eseguire)
                        if (el.historyTask.endTime === null) {
                            //recupero la fase
                        	vm.activeTask = el.historyTask;
                        	utils.refactoringVariables(vm.activeTask);

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

                    $scope.canPublish = response.data.canPublish;
                    $scope.canUpdateAttachments = response.data.canUpdateAttachments;
                    $scope.canSign = false;

                    $scope.isResponsabile = (vm.authorities.includes("ROLE_responsabile-struttura@" + vm.data.entity.variabili.idStruttura) ||
                        vm.authorities.includes("ROLE_responsabile#flussi") ||
                        vm.authorities.includes("ROLE_responsabile#" + vm.data.entity.processDefinitionId.split(':')[0] + "@0000") ||
                        vm.authorities.includes("ROLE_responsabile#" + vm.data.entity.processDefinitionId.split(':')[0] + "@" + vm.data.entity.variabili.idStruttura) ||
                        vm.authorities.includes("ROLE_ADMIN")) 
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


        $scope.history = function(tasks, startTask, initiator) {
            $uibModal.open({
                templateUrl: 'app/pages/details/history.modal.html',
                controller: 'HistoryModalController',
                controllerAs: 'vm',
                size: 'md',
                resolve: {
                    tasks: function() {
                        return tasks;
                    },
                    initiator: function() {
                        return initiator;
                    },
                    startTask: function() {
                        return startTask;
                    }
                }
            })
        };



        $scope.reassign = function(taskId, processInstanceId) {
            $uibModal.open({
                templateUrl: 'app/pages/details/reassign.modal.html',
                controller: 'ReassignModalController',
                controllerAs: 'vm',
                size: 'md',
                resolve: {
                    taskId: function() {
                        return taskId;
                    },
                    processInstanceId: function() {
                        return processInstanceId;
                    }
                }
            })
        };
        
        $scope.deleteProcessInstance = function(processInstanceId) {
            $uibModal.open({
                templateUrl: 'app/pages/details/deleteProcess.modal.html',
                controller: 'DeleteProcessModalController',
                controllerAs: 'vm',
                size: 'md',
                resolve: {
                    processInstanceId: function() {
                        return processInstanceId;
                    }
                }
            })
        };

        $scope.inCart = function (id) {
            return $localStorage.cart && $localStorage.cart.hasOwnProperty(id);
        }

        $scope.addToCart = function(task) {
            $localStorage.cart = $localStorage.cart || {};
            $localStorage.cart[task.id] =  task;
        }

        $scope.removeFromCart = function(task) {
            delete $localStorage.cart[task.id];
            if (Object.keys($localStorage.cart).length == 0) {
                delete $localStorage.cart;
            }
        }
        
        $scope.exportCsv = function() {
            dataService.search
              .exportCsv(vm.searchParams, -1, -1)
              .success(function(response) {
                var filename = "Graduatoria.xls",
                  file = new Blob([response], {
                    type: "application/vnd.ms-excel"
                  });
                $log.info(file, filename);
                saveAs(file, filename);
              });
          };
          
    }
})();