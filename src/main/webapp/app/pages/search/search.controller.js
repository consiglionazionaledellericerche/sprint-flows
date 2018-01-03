(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('SearchController', SearchController);

	SearchController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'AlertService', 'paginationConstants', 'utils', 'workflowTypesDirective', '$log', '$location'];

	function SearchController($scope, $rootScope, $state, dataService, AlertService, paginationConstants, utils, workflowTypesDirective, $log, $location) {
		var vm = this;

		vm.searchParams = $location.search();
		vm.searchParams.active = $location.search().active || true;
		vm.searchParams.order = $location.search().order || "ASC";
		vm.searchParams.page = $location.search().page || 1;
		if ($location.search().isTaskQuery === undefined)
			vm.searchParams.isTaskQuery = false;
		else
			vm.searchParams.isTaskQuery = ($location.search().isTaskQuery == 'true');

                 
		$scope.search = function() {
			
			if (vm.searchParams.processDefinitionKey === null)
				vm.searchParams.processDefinitionKey = undefined;
			
			$log.info(vm.searchParams)
			
			$location.search(vm.searchParams);
			
			dataService.processInstances.search(vm.searchParams)
				.then(function(response) {
					vm.processInstances = utils.refactoringVariables(response.data.processInstances);
					vm.totalItems = response.data.totalItems;
				}, function(response) {
					$log.error(response);
				});
		};

		$scope.stripParams = function() {
			var cleanParams = {};
			cleanParams.active = vm.searchParams.active;
			cleanParams.order  = vm.searchParams.order;
			cleanParams.page   = 1;
			cleanParams.isTaskQuery = vm.searchParams.isTaskQuery;
			cleanParams.processDefinitionKey = vm.searchParams.processDefinitionKey;
			
			vm.searchParams = cleanParams;
		}

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

        $scope.$watchGroup(['vm.searchParams.processDefinitionKey'], function(newVal) {
        	
            if (vm.searchParams.processDefinitionKey) {
                if(vm.searchParams.isTaskQuery) {
                    $scope.formUrl = 'api/forms/'+ vm.searchParams.processDefinitionKey + '/1/search-ti';
                } else {
                    $scope.formUrl = 'api/forms/'+ vm.searchParams.processDefinitionKey + '/1/search-pi';
                }
            } else
                $scope.formUrl = undefined;
            
            $scope.search();
         });
		
		$scope.search();
	}
})();