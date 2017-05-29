(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ActiveFlowsController', ActiveFlowsController);

    ActiveFlowsController.$inject = ['$scope', 'paginationConstants', 'dataService', 'utils', '$log'];

    function ActiveFlowsController($scope, paginationConstants, dataService, utils, $log) {
        var vm = this;
        vm.order = 'ASC';
        //variabili per la paginazione delle due pagine (terminatedTask e myTask)
        vm.terminatedPage = 1;
        vm.activePage = 1;
        // JSON che conterrà le ProcessInstances ATTIVE delle due query
        vm.activeInstances = {
            total: 0
        };
        // JSON che conterrà le ProcessInstances TERMINATE delle due query
        vm.terminatedProcess = {
            total: 0
        };

        $scope.loadActiveProcess = function() {
            //variabili usate nella paginazione
            var activeFirstResult, activeMaxResults;
            vm.itemsPerPage = paginationConstants.itemsPerPage;
            vm.activeTotalItems = vm.itemsPerPage * vm.activePage;
            activeFirstResult = vm.itemsPerPage * (vm.activePage - 1);
            activeMaxResults = vm.itemsPerPage;

            dataService.processInstances.getProcessInstances($scope.current, true, activeFirstResult, activeMaxResults, vm.order, utils.populateProcessParams(Array.from($("input[id^='searchField-']"))))
                .then(function(response) {
                    vm.activeProcess = utils.refactoringVariables(response.data.data);
                    // variabili per la gestione della paginazione
                    vm.activeTotalItems = response.data.total;
                    vm.activeQueryCount = vm.activeTotalItems;
                }, function(response) {
                    $log.error(response);
                });
        };


        $scope.loadTerminatedProcess = function() {
            //variabili usate nella paginazione
            var firstResultTerminated, maxResultsTerminated;
            vm.itemsPerPage = paginationConstants.itemsPerPage;
            vm.terminatedTotalItems = vm.itemsPerPage * vm.terminatedPage;
            firstResultTerminated = vm.itemsPerPage * (vm.terminatedPage - 1);
            maxResultsTerminated = vm.itemsPerPage;

            //buono (da adattare)
            dataService.processInstances.getProcessInstances($scope.current, false, firstResultTerminated, maxResultsTerminated, vm.order, utils.populateProcessParams(Array.from($("input[id^='searchField-']"))))
                .then(function(response) {
                    vm.terminatedProcess = utils.refactoringVariables(response.data.data);
                    // variabili per la gestione della paginazione
                    vm.terminatedTotalItems = response.data.total;
                    vm.terminatedQueryCount = vm.terminatedTotalItems;
                }, function(response) {
                    $log.error(response);
                });
        };



        $scope.showProcessInstances = function(requestedPage) {
            //se non ho ancora effettuato query carico entrambe le "viste" ("i miei task" ed i "task di gruppo")
            //todo: se si vuole caricare entrambe le pagine ogni volta basta togliere l'if/else ed eseguire sempre il codice nell'if
            if (vm.terminatedProcess.total === 0 && vm.activeInstances.total === 0) {
                $scope.loadActiveProcess();
                $scope.loadTerminatedProcess();
            } else {
                if (vm.activeContent == 'active') {
                    $scope.loadActiveProcess();
                } else if (vm.activeContent == 'terminated') {
                    $scope.loadTerminatedProcess();
                }
            }
        };

        $scope.setActiveContent = function(choice) {
            vm.activeContent = choice;
        };

        //aggiornamento pagina in caso di cambio "ordinamento" o Process definition
        $scope.$watchGroup(['vm.order'], function() {
            $scope.showProcessInstances()
        });

        //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        vm.transition = function transition() {
            $scope.showProcessInstances()
        };
    }
})();