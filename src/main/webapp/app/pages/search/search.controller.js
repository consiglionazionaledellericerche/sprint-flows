(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('SearchController', SearchController);

	SearchController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'AlertService', 'paginationConstants', 'utils', 'workflowTypesDirective', '$log'];

	function SearchController($scope, $rootScope, $state, dataService, AlertService, paginationConstants, utils, workflowTypesDirective, $log) {
		var vm = this;

		$rootScope.isTaskQuery = false;
		vm.availableFilter = $rootScope.availableFilter;
		vm.order = 'ASC';
		vm.active = true;
		//serve per resettare la label della tipologia di Process Definition scelta in caso di passaggio "temporaneo" in un'altra pagina
		$rootScope.current = undefined;
		//variabili usate nella paginazione
		vm.itemsPerPage = paginationConstants.itemsPerPage;
		vm.transition = transition;
		vm.page = 1;
		vm.totalItems = vm.itemsPerPage * vm.page;

		// Reload ricerca in caso di modifica dell'ordine di visualizzazione (crescente/decrescente)
		$scope.$watchGroup(['vm.order'], function() {
			$scope.search();
		});


		$scope.showProcessInstances = function(active) {
			vm.active = active;
			$scope.search();
		};


		$scope.search = function() {
			var maxResults = vm.itemsPerPage,
				firstResult = vm.itemsPerPage * (vm.page - 1);

			if ($rootScope.isTaskQuery) {
				dataService.tasks.search($scope.current, vm.active, utils.populateTaskParams(Array.from($("input[id^='searchField-']"))), vm.order, firstResult, maxResults)
					.then(function(response) {
						vm.taskInstances = utils.refactoringVariables(response.data.tasks);
						// variabili per la gestione della paginazione
						vm.totalItems = response.data.totalItems;
						vm.queryCount = vm.totalItems;
					}, function(response) {
						$log.error(response);
					});
			} else {
				//                dataService.processInstances.search($scope.current, vm.active, exstractSearchParams(), vm.order, firstResult, maxResults)
				dataService.processInstances.search($scope.current, vm.active, utils.populateProcessParams(Array.from($("input[id^='searchField-']"))), vm.order, firstResult, maxResults)
					.then(function(response) {
						vm.processInstances = utils.refactoringVariables(response.data.processInstances);
						// variabili per la gestione della paginazione
						vm.totalItems = response.data.totalItems;
						vm.queryCount = vm.totalItems;
					}, function(response) {
						$log.error(response);
					});
			}
		};


		$scope.switch = function(isTaskQuery) {
			$rootScope.isTaskQuery = isTaskQuery;
			$rootScope.current = undefined;
		};



		$scope.exportCsv = function() {
			if ($rootScope.isTaskQuery) {
				dataService.tasks.exportCsv($scope.current, vm.active, utils.populateTaskParams(Array.from($("input[id^='searchField-']"))), vm.order, -1, -1)
					.success(function(response) {
						var filename = new Date().toISOString().slice(0, 10) + ".xls",
							file = new Blob([response], {
								type: 'application/vnd.ms-excel'
							});
						$log.info(file, filename);
						saveAs(file, filename);
					});
			} else {
				dataService.processInstances.exportCsv($scope.current, vm.active, utils.populateProcessParams(Array.from($("input[id^='searchField-']"))), vm.order, -1, -1)
					.success(function(response) {
						var filename = new Date().toISOString().slice(0, 10) + ".xls",
							file = new Blob([response], {
								type: 'application/vnd.ms-excel'
							});
						$log.info(file, filename);
						saveAs(file, filename);
					});
			}
		};



		//funzione richiamata quando si chiede una nuova "pagina" dei risultati
		function transition() {
			$scope.search();
		}
	}
})();