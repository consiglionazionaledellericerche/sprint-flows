(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('SearchController', SearchController);

	SearchController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'AlertService', 'paginationConstants', 'utils', '$log', '$location'];

	function SearchController($scope, $rootScope, $state, dataService, AlertService, paginationConstants, utils, $log, $location) {
		var vm = this,
			oldUrl = $scope.formUrl;

		$scope.reload = false;

		vm.searchParams = $location.search();
		vm.searchParams.active = $location.search().active || true;
		vm.searchParams.order = $location.search().order || "ASC";
		vm.searchParams.page = $location.search().page || 1;
		vm.searchParams.processDefinitionKey = 'all';
		if ($location.search().isTaskQuery === undefined)
			vm.searchParams.isTaskQuery = false;
		else
			vm.searchParams.isTaskQuery = ($location.search().isTaskQuery === true || $location.search().isTaskQuery == 'true');

		$scope.hasPendingRequests = function() {
			return httpRequestTracker.hasPendingRequests();
		};

		$scope.search = function() {
			//serve per evitare di ricaricare le form di ricerca associate alla Process Definition ad ogni nuova ricerca
			$scope.reload = $scope.formUrl !== oldUrl;

			vm.results = [];
			vm.loading = true;

			if (vm.searchParams.processDefinitionKey === null)
				vm.searchParams.processDefinitionKey = undefined;

			$log.info(vm.searchParams)

			$location.search(vm.searchParams);

			dataService.processInstances.search(vm.searchParams)
				.then(function(response) {
					if (vm.searchParams.isTaskQuery)
						vm.results = utils.refactoringVariables(response.data.tasks);
					else
						vm.results = utils.refactoringVariables(response.data.processInstances);

					vm.totalItems = response.data.totalItems;
					vm.loading = false;

				}, function(response) {
					$log.error(response);
					vm.loading = false;
				});
		};

		$scope.stripParams = function() {
			var cleanParams = {};
			cleanParams.active = vm.searchParams.active;
			cleanParams.order = vm.searchParams.order;
			cleanParams.page = 1;
			cleanParams.isTaskQuery = vm.searchParams.isTaskQuery;
			cleanParams.processDefinitionKey = vm.searchParams.processDefinitionKey;

			vm.searchParams = cleanParams;
		}

		$scope.exportCsv = function() {
			dataService.search.exportCsv(vm.searchParams, -1, -1)
				.success(function(response) {
					var filename = new Date().toISOString().slice(0, 10) + ".xls",
						file = new Blob([response], {
							type: 'application/vnd.ms-excel'
						});
					$log.info(file, filename);
					saveAs(file, filename);
				});
		};

		$scope.$watchGroup(['vm.searchParams.processDefinitionKey'], function(newVal) {

			if (vm.searchParams.processDefinitionKey) {
				if (vm.searchParams.isTaskQuery) {
					$scope.formUrl = 'api/forms/' + vm.searchParams.processDefinitionKey + '/1/search-ti';
				} else {
					$scope.formUrl = 'api/forms/' + vm.searchParams.processDefinitionKey + '/1/search-pi';
				}
			} else
				$scope.formUrl = undefined;
		});

		$scope.search();
	}
})();