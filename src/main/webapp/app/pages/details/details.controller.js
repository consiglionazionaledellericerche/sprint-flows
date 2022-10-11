(function() {
	'use strict';

	angular.module('sprintApp').controller('DetailsController', DetailsController);

	DetailsController.$inject = ['$scope', '$rootScope', 'Principal', '$state', '$localStorage', 'dataService', 'AlertService', '$log', 'utils', '$uibModal'];

	function DetailsController($scope, $rootScope, Principal, $state, $localStorage, dataService, AlertService, $log, utils, $uibModal) {
		$scope.button={};
		$scope.button.disabled = true;
		var vm = this, activeTaskVariables;
		vm.searchParams = {};
		vm.data = {};
		vm.taskId = $state.params.taskId;
		//vm.searchParams.statoFinaleDomanda = {};
		vm.showGerarchia = false;
		vm.searchParams.active = true;
		vm.searchParams.order = "ASC";
		vm.searchParams.page = 1;
		vm.searchParams.processDefinitionKey = "short-term-mobility-domande";
		vm.searchParams.processInstanceId = "0";
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
//						vm.data.history = response.data.history;
						//in response.data.entity.variables ci sono anche le properties della Process Instance (initiator, startdate, ecc.)
						vm.data.startEvent = response.data.entity.variables;
						vm.data.attachments = utils.parseAttachments(response.data.attachments);
						vm.data.identityLinks = response.data.identityLinks;
						vm.diagramUrl = '/rest/diagram/processInstance/' + vm.data.entity.id + "?" + new Date().getTime();
						vm.data.businessKey = response.data.entity.businessKey;

						vm.data.history = response.data.history;
						//aggiungo le variabili del task (servono quando si aggiunge il task al carrello firma)
						activeTaskVariables = response.data.entity.variabili;
                        
						// manage info for parallel task in the same processInstance
                        vm.activeTasks = [];	
						var processInstanceStateMultiTask = "";					
						vm.data.history.forEach(function(el) {
							//recupero l'ultimo task (quello ancora da eseguire)...
							if (el.historyTask.endTime === null) {
								vm.activeTask = el.historyTask;
								//...e gli metto le variabili
								vm.activeTask.variabili = activeTaskVariables;
								
								// save all activeTasks for use it in a parallel task environment
                                vm.activeTasks.push(el.historyTask);

								if(!processInstanceStateMultiTask.includes(el.historyTask.name)){
								  processInstanceStateMultiTask = processInstanceStateMultiTask + '@' + el.historyTask.name;
								}
							}
						})
                        // manage state of the process -sum of the name of the active tasks-
						vm.data.entity.processInstanceStateName = processInstanceStateMultiTask; 

						var processDefinition = response.data.entity.processDefinitionId.split(":");
//						var stato = response.data.history[0].historyTask.name;
						var stato = JSON.parse(response.data.entity.name).stato;

						vm.detailsView = 'api/views/' + processDefinition[0] + '/' + processDefinition[1] + '/detail';

						if(vm.data.entity.variabili.valutazioneEsperienze_json){
							vm.experiences = jQuery.parseJSON(vm.data.entity.variabili.valutazioneEsperienze_json);
						}
						if(processDefinition[0] != null
								& 
								((processDefinition[0] == "short-term-mobility-bando-dipartimento" & stato == "PROVVEDIMENTO GRADUATORIA")
										||
										(processDefinition[0] == "laboratori-congiunti-bando-dipartimento" & stato == "PROVVEDIMENTO GRADUATORIA"))
						) {
							if(processDefinition[0] == "laboratori-congiunti-bando-dipartimento"){
								vm.searchParams.processDefinitionKey = "laboratori-congiunti-domande";
							}
							vm.showGerarchia = true;
						}

						if(response.data.variabili.idBando){
							vm.searchParams.idBando = "text="+response.data.variabili.idBando.value;
						}
						if(response.data.variabili.dipartimentoId){
							vm.searchParams.dipartimentoId = "text="+response.data.variabili.dipartimentoId.value;
						}
						vm.searchParams.processInstanceId = response.data.variabili.processInstanceId.value;

						$scope.canPublish = response.data.canPublish;
						$scope.canUpdateAttachments = response.data.canUpdateAttachments;
						$scope.canSign = false;

						$scope.isResponsabile = (vm.authorities.includes("ROLE_responsabile#flussi") ||
								vm.authorities.includes("ROLE_responsabile#" + vm.data.entity.processDefinitionId.split(':')[0] + "@0000") ||
								vm.authorities.includes("ROLE_responsabile#" + vm.data.entity.processDefinitionId.split(':')[0] + "@" + vm.data.entity.variabili.idStruttura) ||
								vm.authorities.includes("ROLE_ADMIN"));

						$scope.isRevocabile = response.data.isRevocabile;

						//riattivo il bottone delle "azioni"
						$scope.button.disabled = false;
					}
			);
		}


		$scope.exportSummary = function(url, filename) {
			utils.downloadFile(url, filename, 'application/pdf');
		};

		$scope.avviaFlussoRevoca = function() {
			$uibModal.open({
				templateUrl: 'app/pages/details/revoca.modal.html',
				controller: 'RevocaModalController',
				controllerAs: 'vm',
				size: 'md',
				resolve: {
					processInstanceId: function() {
						return $scope.processInstanceId;
					}
				}
			})
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


		$scope.history = function(processInstanceId, startTask, initiator) {
			$uibModal.open({
				templateUrl: 'app/pages/details/history.modal.html',
				controller: 'HistoryModalController',
				controllerAs: 'vm',
				size: 'md',
				resolve: {
					processInstanceId: function() {
						return $scope.processInstanceId;
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
			.exportCsvAndSaveInProcess(vm.searchParams, -1, -1)
			.success(function(response) {
				AlertService.success("File Graduatoria Inserito correttamente nel fascicolo del Flusso");
			});
		};

	}
})();