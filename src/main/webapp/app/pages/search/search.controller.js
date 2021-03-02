(function() {
  'use strict';

  angular.module("sprintApp").controller("SearchController", SearchController);

  SearchController.$inject = ["$scope", "dataService", "utils", "$log", "$location"];

  function SearchController($scope, dataService, utils, $log, $location) {
    var vm = this,
      oldUrl = $scope.formUrl;


    $scope.reload = false;
    // "conservo" i parametri della ricerca  ...
    vm.searchParams = $location.search();
    vm.searchParams.active = $location.search().active || true;
    vm.searchParams.order = $location.search().order || "ASC";
    if(!vm.searchParams.page)
      vm.page = vm.searchParams.page = $location.search().page || 1;
    vm.searchParams.processDefinitionKey = $location.search().processDefinitionKey || "all";

    $scope.search = function() {
      //serve per evitare di ricaricare le form di ricerca associate alla Process Definition ad ogni nuova ricerca
      $scope.reload = $scope.formUrl !== oldUrl;

      vm.results = [];
      vm.loading = true;

      if (vm.searchParams.processDefinitionKey === null)
        vm.searchParams.processDefinitionKey = undefined;

      $log.info(vm.searchParams);

      $location.search(vm.searchParams);

      //ripulisco i valori con null(tipo quando annullo la data selezionata)
      Object.keys(vm.searchParams).forEach(function(key) {
          if(vm.searchParams[key] == null)
            delete vm.searchParams[key];
      });
      dataService.processInstances.search(vm.searchParams).then(
        function(response) {
          vm.results = utils.refactoringVariables(response.data.data);

          vm.totalItems = response.data.total;
          vm.loading = false;
        },
        function(response) {
          $log.error(response);
          vm.loading = false;
        }
      );
    };


    $scope.exportCsv = function() {
      dataService.search
        .exportCsv(vm.searchParams, -1, -1)
        .success(function(response) {
          var filename = new Date().toISOString().slice(0, 10) + ".xls",
            file = new Blob([response], {
              type: "application/vnd.ms-excel"
            });
          $log.info(file, filename);
          saveAs(file, filename);
        });
    };

    $scope.$watchGroup(
      ["vm.searchParams.processDefinitionKey"],
      function() {
        if (vm.searchParams.processDefinitionKey) {
            $scope.formUrl =
              "api/forms/" +
              vm.searchParams.processDefinitionKey +
              "/1/search-pi";
        } else {
          $scope.formUrl = "api/forms/all/1/search-pi";
        }
      }
    );


    $scope.search();
  }
})();
