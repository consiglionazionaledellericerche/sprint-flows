(function() {
  "use strict";

  angular
    .module("sprintApp")
    .controller("AvailableTasksController", AvailableTasksController);

  AvailableTasksController.$inject = ["$scope", "paginationConstants", "dataService", "utils", "$log", "$location"];

  function AvailableTasksController($scope, paginationConstants, dataService, utils, $log, $location) {
    var vm = this;
    //Carico i parametri di ricerca "salvati" se torno dlla pagine dei "details"
    vm.searchParams = $location.search();
    vm.searchParams.active = $location.search().active || true;
    vm.searchParams.order = $location.search().order || "ASC";
    vm.searchParams.page = $location.search().page || 1;
    vm.searchParams.processDefinitionKey = $location.search().processDefinitionKey || "all";

    vm.order = "ASC";
    // variabili per la paginazione delle due pagine (availableTask e myTask)
    vm.myPage = 1;
    vm.availablePage = 1;
    vm.TAIMGPage = 1;
    // JSON che conterrà i risultati delle due query
    vm.myTasks = {
      total: 0
    };
    vm.availableTasks = {
      total: 0
    };
    vm.taskAssignedInMyGroups = {
      total: 0
    };

    $scope.loadMyTasks = function() {
      // variabili usate nella paginazione
      var myFirstResult,
        myMaxResults;

      // carico le form di ricerca specifiche per ogni tipologia di Process Definitions
      $scope.formUrl = utils.loadSearchFields(vm.processDefinitionKey, true);

      vm.itemsPerPage = paginationConstants.itemsPerPage;
      vm.myTotalItems = vm.itemsPerPage * vm.myPage;
      myFirstResult = vm.itemsPerPage * (vm.myPage - 1);
      myMaxResults = vm.itemsPerPage;

      dataService.tasks
        .myTasks(
          vm.processDefinitionKey,
          myFirstResult,
          myMaxResults,
          vm.order,
          utils.populateTaskParams(vm.searchParams)
        )
        .then(
          function(response) {
            utils.refactoringVariables(response.data.data);
            vm.myTasks = response.data;
            // variabili per la gestione della paginazione
            vm.myTotalItems = response.data.total;
            vm.myQueryCount = vm.myTotalItems;
          },
          function(response) {
            $log.error(response);
          }
        );
    };

    $scope.loadAvailableTasks = function() {
      // variabili usate nella paginazione
      var firstResultAvailable, maxResultsAvailable;
      vm.itemsPerPage = paginationConstants.itemsPerPage;
      vm.availableTotalItems = vm.itemsPerPage * vm.availablePage;
      firstResultAvailable = vm.itemsPerPage * (vm.availablePage - 1);
      maxResultsAvailable = vm.itemsPerPage;

      dataService.tasks
        .myTasksAvailable(
          vm.processDefinitionKey,
          firstResultAvailable,
          maxResultsAvailable,
          vm.order,
          utils.populateTaskParams(vm.searchParams)
        )
        .then(
          function(response) {
            utils.refactoringVariables(response.data.data);
            vm.availableTasks = response.data;
            // variabili per la gestione della paginazione
            vm.availableTotalItems = response.data.total;
            vm.availableQueryCount = vm.availableTotalItems;
          },
          function(response) {
            $log.error(response);
          }
        );
    };

    $scope.loadTaskAssignedInMyGroups = function() {
      // variabili usate nella paginazione
      var firstResultTAIMG, maxResultsTAIMG;
      vm.itemsPerPage = paginationConstants.itemsPerPage;
      vm.TAIMGTotalItems = vm.itemsPerPage * vm.availablePage;
      firstResultTAIMG = vm.itemsPerPage * (vm.availablePage - 1);
      maxResultsTAIMG = vm.itemsPerPage;

      dataService.tasks
        .taskAssignedInMyGroups(
          vm.processDefinitionKey,
          firstResultTAIMG,
          maxResultsTAIMG,
          vm.order,
          utils.populateTaskParams(vm.searchParams)
        )
        .then(
          function(response) {
            utils.refactoringVariables(response.data.data);
            vm.taskAssignedInMyGroups = response.data;
            // variabili per la gestione della paginazione
            vm.TAIMGTotalItems = response.data.total;
            vm.TAIMGQueryCount = vm.TAIMGTotalItems;
          },
          function(response) {
            $log.error(response);
          }
        );
    };

    $scope.showProcessInstances = function(requestedPage) {
      // se non ho ancora effettuato query carico entrambe le "viste" ("i miei task" ed i "task di gruppo")
      if (vm.availableTasks.total === 0 && vm.myTasks.total === 0) {
        $scope.loadTasks();
      } else {
        switch (vm.activeContent) {
          case "myTasks":
            $scope.loadMyTasks();
            break;
          case "availables":
            $scope.loadAvailableTasks();
            break;
          case "taskAssignedInMyGroups":
            $scope.loadTaskAssignedInMyGroups();
            break;
        }
      }
    };

    $scope.loadTasks = function() {
      //"salvo" i parametri di ricerca
      $location.search(vm.searchParams);
      $scope.loadMyTasks();
      $scope.loadAvailableTasks();
      $scope.loadTaskAssignedInMyGroups();
    };

    $scope.setActiveContent = function(choice) {
      vm.activeContent = choice;
    };

    // aggiornamento pagina in caso di cambio "ordinamento" o Process definition
    $scope.$watchGroup(["vm.order"], function() {
      $scope.showProcessInstances();
    });

    // funzione richiamata quando si chiede una nuova "pagina" dei risultati
    vm.transition = function transition() {
      $scope.showProcessInstances();
    };
  }
})();
