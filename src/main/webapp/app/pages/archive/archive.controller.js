(function() {
  'use strict';

  angular.module("sprintApp").controller("ArchiveController", ArchiveController);

  ArchiveController.$inject = ["$scope", "dataService", "utils", "$log", "$location"];

  function ArchiveController($scope, dataService, utils, $log, $location) {
    var vm = this,
      oldUrl = $scope.formUrl;

    $scope.reload = false;

    vm.searchParams = $location.search();
    vm.searchParams.active = $location.search().active || true;
    vm.searchParams.order = $location.search().order || "ASC";
    vm.searchParams.page = $location.search().page || 1;
    vm.searchParams.processDefinitionKey = "all";

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
      dataService.archive.search(vm.searchParams).then(
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

    $scope.$watchGroup(
      ["vm.searchParams.processDefinitionKey"],
      function() {
        if (vm.searchParams.processDefinitionKey) {
            $scope.formUrl =
              "api/forms/" +
              vm.searchParams.processDefinitionKey +
              "/1/search-pi";
        } else {
          $scope.formUrl = undefined;
        }
      }
    );


    $scope.search();
  }
})();
