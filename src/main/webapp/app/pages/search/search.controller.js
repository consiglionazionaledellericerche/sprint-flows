(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('SearchController', SearchController);

	SearchController.$inject = ['$scope', '$state', 'dataService', 'AlertService', 'paginationConstants', 'utils', 'workflowTypesDirective', '$log', '$location'];

	function SearchController($scope, $state, dataService, AlertService, paginationConstants, utils, workflowTypesDirective, $log, $location) {
		var vm = this;
		vm.searchTerms = {};

// 		//variabili usate nella paginazione
// 		vm.transition = transition;

		vm.searchTerms = $location.search();
 		vm.itemsPerPage = vm.itemsPerPage || paginationConstants.itemsPerPage;
		vm.page = vm.page || 1;
		vm.totalItems = vm.itemsPerPage * vm.page;
		vm.searchTerms.active = vm.active || true;
		vm.searchTerms.order = vm.searchTerms.order || "ASC";
	

		$scope.search = function() {
			
			vm.maxResults = vm.itemsPerPage;
			vm.firstResult = vm.itemsPerPage * (vm.page - 1);

			angular.extend(vm.searchTerms, utils.populateProcessParams(Array.from($("input[id^='searchField-']"))) );
			vm.searchTerms.processDefinitionKey = $scope.current;
			
			$log.info(vm.searchTerms);
			$location.search(vm.searchTerms);
			
			dataService.processInstances.search(vm.searchTerms)
				.then(function(response) {
					vm.processInstances = utils.refactoringVariables(response.data.processInstances);
					// variabili per la gestione della paginazione
					vm.totalItems = response.data.totalItems;
					vm.queryCount = vm.totalItems;
				}, function(response) {
					$log.error(response);
				});
		};



		$scope.switch = function(isTaskQuery) {
			$scope.isTaskQuery = isTaskQuery;
			$scope.current = undefined;
		};


		$scope.exportCsv = function() {
			if ($scope.isTaskQuery) {
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
		
		$scope.search();
	}
})();