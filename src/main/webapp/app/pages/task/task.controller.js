(function () {
	'use strict';

	angular
	.module('sprintApp')
	.controller('TaskController', TaskController);

	/**
	 * Questo e' un po' il cuore di tutta l'applicazione, per questo e' un pochino piu' complicato di altri
	 * Innanzitutto c'e' una promise composta che aspetta che sia la form che le variabili siano caricate,
	 * e a quel punto valoriza i campi della form ove richiesto con 'autofill'.
	 *
	 * Al momento dell'invio della form succedono piu' cose un po' complicate per un motivo specifico:
	 * per permettere l'atomicita' delle azioni, i files sono trattati come metadati e vengono inviati
	 * insieme alla form degli altri metadati, e non in un momento separato come era nella vecchia Scrivania.
	 * Per fare questo usiamo form Multipart, e una libreria angular che gestisce l'invio dei file (wrappata nel nostro fileinput)
	 * Questo richiede alcuni accorgimenti, perche' possiamo inviare o JSON o Multipart con files
	 *
	 * Per questo, al momento dell'invio, campi complessi (subform, campi multipli) vengono serializzati in stringhe.
	 * Nel fare questo vanno copiati in una variabile nuova, senno l'UI sballa, e filtrati lato server (FlowsTaskResource.extractParameters())
	 *
	 * NB: non posso usare angular.copy() o altrimenti duplicare i dati per la submit
	 *     perche' non vengono gestiti bene gli oggetti del nuovo tipo File/Blob
	 *     Sono costretto a inviare il $scope.data originale
	 *
	 * @author mtrycz
	 */
	TaskController.$inject = ['$scope', '$rootScope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log', '$http', '$q', 'Upload', 'utils', '$localStorage', '$uibModal'];

	function TaskController($scope, $rootScope, Principal, LoginService, $state, dataService, AlertService, $log, $http, $q, Upload, utils, $localStorage, $uibModal) {
		var vm = this, deploymentId, isShared = false;

        $scope.button={};
		$scope.data = {};
		vm.taskId = $state.params.taskId;
		$scope.taskId = $state.params.taskId;
		$scope.data.processDefinitionId = $state.params.processDefinitionId;
		$scope.processDefinitionKey = $scope.data.processDefinitionId.split(":")[0];
		$scope.processVersion = $scope.data.processDefinitionId.split(":")[1];
		$scope.attachments = {};
		$scope.account = {}
		
		Principal.identity().then(function(account) {
                $scope.account = account;
        })

		// Ho bisogno di caricare piu' risorse contemporaneamente (form e data);
		// quando sono finite entrambe, autofillo la form
		var formPromise = $q.defer(),
		dataPromise = $q.defer();
		$scope.autofill = function () {
			formPromise.resolve(2);
		}; // usato nell'html


		$q.all([formPromise.promise, dataPromise.promise])
		.then(function (value) {
			angular.forEach(taskForm, function (el) {
				if (el.attributes.autofill)
					$scope.data[el.id] = vm.taskVariables[el.id];
			});
			//Autofill dei campi che, essendo caricati solo in alcuni casi specifici, non vengono valorizzati a questo punto nella form
			if ([11, 12, 13, 15, 16, 18, 21, 22, 23].includes(Number(vm.taskVariables["tipologiaAcquisizioneId"])))
				$scope.data["strumentoAcquisizione"] = vm.taskVariables["strumentoAcquisizione"];
			if ([14, 17].includes(Number(vm.taskVariables["tipologiaAcquisizioneId"])))
				$scope.data["strumentoAcquisizioneId"] = vm.taskVariables["strumentoAcquisizioneId"];
			if ([11, 12, 13].includes(Number(vm.taskVariables["strumentoAcquisizioneId"])))
				$scope.data["tipologiaAffidamentoDiretto"] = vm.taskVariables["tipologiaAffidamentoDiretto"];
			if ([21, 23].includes(Number(vm.taskVariables["strumentoAcquisizioneId"])))
				$scope.data["tipologiaProceduraSelettiva"] = vm.taskVariables["tipologiaProceduraSelettiva"];

            if($state.params.taskId){
                if($state.params.processDefinitionId.includes('short-term-mobility-domande'))
                    isShared = true;

                dataService.draft.getDraftByTaskId($state.params.taskId, isShared).then(
                        function(response){
                            //popolo i campi col contenuto del json
                            var json = JSON.parse(response.data.json);
                            Object.keys(json).forEach(function(key) {
                                $scope.data["" + key] = json[key];
                            })
                        }
                );
            } else{
                dataService.draft.getDraftByProcessDefinitionId($state.params.processDefinitionId.split(':')[0]).then(
                        function(response){
                            //popolo i campi col contenuto del json
                            var json = JSON.parse(response.data.json);
                            Object.keys(json).forEach(function(key) {
                                $scope.data["" + key] = json[key];
                            })
                        }
                );
            }
		});

		if ($state.params.taskId) {
			dataService.tasks.getTask($state.params.taskId).then(
				function (response) {
					$scope.data.taskId = $state.params.taskId;
					//visualizzazione dei metadati del task in esecuzione
					var processDefinition = response.data.task.processDefinitionId.split(":");
					vm.detailsView = 'api/views/' + processDefinition[0] + '/' + processDefinition[1] + '/detail';
					$scope.data.entity = utils.refactoringVariables([response.data.task])[0];

					vm.taskVariables = $scope.data.entity.variabili;
					$scope.attachments = utils.parseAttachments(response.data.attachments);
					//                    $scope.attachments = response.data.attachments;

					vm.diagramUrl = '/rest/diagram/taskInstance/' + $scope.data.taskId + "?" + new Date().getTime();
					vm.formUrl = 'api/forms/task/' + $scope.data.taskId;
					dataPromise.resolve();
				});
		} else {
//		    deploymentId = $localStorage.wfDefsAll.filter(function(el){return el.id == $state.params.processDefinitionId})[0].deploymentId;
//		    autofill draft (deploymentId(negativo))
		    dataService.draft.getDraftByProcessDefinitionId($state.params.processDefinitionId.split(':')[0]).then(
				function(response){
					//popolo i campi col contenuto del json
					var json = JSON.parse(response.data.json);
					Object.keys(json).forEach(function(key) {
						$scope.data["" + key] = json[key];
					})
				}
            );

			dataPromise.reject("");

			vm.diagramUrl = "/rest/diagram/processDefinition/" + $state.params.processDefinitionId + "?" + new Date().getTime();
			vm.formUrl = 'api/forms/' + $scope.processDefinitionKey + "/" + $scope.processVersion + "/" + $state.params.taskName
		}

		$scope.preSubmitTask = function (file) {
		
		    if ($scope.taskForm.$invalid) {
                angular.forEach($scope.taskForm.$error, function (field) {
                    angular.forEach(field, function (errorField) {
                        errorField.$setTouched();
                    });
                });
                $("#confirmModal").hide() //rimuovo la modale di conferma
                AlertService.warning("Inserire tutti i valori obbligatori.");
                //$scope.button.disabled = false;

            } else {
                if($rootScope.inDevelopment){
                    $scope.submitTask(file);
                } else {
                    $uibModal.open({
                        templateUrl: 'confirmModal.html',
                        scope: $scope
                    });
                }
            }
		};

		$scope.submitTask = function (file) {
		    $scope.button.disabled = true;

			if ($scope.taskForm.$invalid) {
				angular.forEach($scope.taskForm.$error, function (field) {
					angular.forEach(field, function (errorField) {
						errorField.$setTouched();
					});
				});
				$("#confirmModal").hide() //rimuovo la modale di conferma
				AlertService.warning("Inserire tutti i valori obbligatori.");
				//$scope.button.disabled = false;

			} else {
			    $("#confirmModal").hide() //rimuovo la modale di conferma
				// Serializzo gli oggetti complessi in stringhe
				// E' necessario copiarli in un nuovo campo, senno' angular si incasina
				// Non posso usare angular.copy() perche' ho degli oggetti File non gestiti bene
				utils.prepareForSubmit($scope.data, $scope.attachments);

				Upload.upload({
					url: 'api/tasks/complete',
					data: $scope.data,
				}).then(function (response) {

					$log.info(response);
					AlertService.success("Richiesta completata con successo");
					removeFromCart($state.params.taskId)
					$state.go('availableTasks');

        		    $scope.button.disabled = false;
				}, function (err) {
					$log.error(err);
					if (err.status == 412) {
						AlertService.warning("AVVISO<br>" + err.data.message);
					} else if (err.status == -1) {
						AlertService.error("Richiesta non riuscita<br>" + "E' possibile che la richiesta superi il limite massimo di grandezza (50MB)");
					} else {
						AlertService.error("Richiesta non riuscita<br>" + err.data.message);
					}
				    $scope.button.disabled = false;
				});
			}
		}

		$scope.downloadFile = function (url, filename, mimetype) {
			utils.downloadFile(url, filename, mimetype);
		}

		$scope.createDraft = function (taskId) {
			//copio scope.data e tolgo i campi che non voglio salvare nel Draft
			var json = Object.assign({}, $scope.data);
			delete json.entity;
			delete json.processDefinitionId;
//			delete json.sceltaUtente;
			delete json.taskId;
			//salvo il draft
			//todo: da testare
			if($state.params.processDefinitionId.includes('short-term-mobility-domande'))
			    isShared = true;

			if($state.params.taskId){
			    dataService.draft.updateDraft($state.params.taskId , json, null, isShared);
			} else{
			    dataService.draft.updateDraft(null, json, $state.params.processDefinitionId.split(':')[0], isShared);
			}
		}

		function removeFromCart(taskId) {
			if (taskId && $localStorage.cart) {
				delete $localStorage.cart[taskId];
				if (Object.keys($localStorage.cart).length == 0) {
					delete $localStorage.cart;
				}
			}
		}
	}
})();
