(function () {
    'use strict';

    angular
        .module('sprintApp')
        .controller('AvailableTasksController', AvailableTasksController);

    AvailableTasksController.$inject = ['$scope', 'paginationConstants', 'dataService', 'utils', '$log'];

    function AvailableTasksController($scope, paginationConstants, dataService, utils, $log) {
        var vm = this;
        vm.order = 'ASC';
        //variabili per la paginazione delle due pagine (availableTask e myTask)
        vm.availablePage = 1;
        vm.myPage = 1;
        // JSON che conterrà i risultati delle due query
        vm.myTasks = {
            total: 0
        };
        vm.availableTasks = {
            total: 0
        };

        $scope.loadMyTasks = function() {
            //variabili usate nella paginazione
            var myFirstResult, myMaxResults;
            vm.itemsPerPage = paginationConstants.itemsPerPage;
            vm.myTotalItems = vm.itemsPerPage * vm.myPage;
            myFirstResult = vm.itemsPerPage * (vm.myPage - 1);
            myMaxResults = vm.itemsPerPage;

            dataService.tasks.myTasks($scope.current, myFirstResult, myMaxResults, vm.order, populateParams())
                .then(function(response) {
                    utils.refactoringVariables(response.data.data);
                    vm.myTasks = response.data;
                    // variabili per la gestione della paginazione
                    vm.myTotalItems = response.data.total;
                    vm.myQueryCount = vm.myTotalItems;
                }, function(response) {
                    $log.error(response);
                });
        };


        $scope.loadAvailableTasks = function() {
            //variabili usate nella paginazione
            var firstResultAvailable, maxResultsAvailable;
            vm.itemsPerPage = paginationConstants.itemsPerPage;
            vm.availableTotalItems = vm.itemsPerPage * vm.availablePage;
            firstResultAvailable = vm.itemsPerPage * (vm.availablePage - 1);
            maxResultsAvailable = vm.itemsPerPage;
            dataService.tasks.myTasksAvailable($scope.current, firstResultAvailable, maxResultsAvailable, vm.order, populateParams())
                .then(function(response) {
                    utils.refactoringVariables(response.data.data);
                    vm.availableTasks = response.data;
                    // variabili per la gestione della paginazione
                    vm.availableTotalItems = response.data.total;
                    vm.availableQueryCount = vm.availableTotalItems;
                }, function(response) {
                    $log.error(response);
                });


        };

        function populateParams() {
            var fields = Array.from($("input[id^='searchField-']")), processParams = [], taskParams = [];

            fields.forEach(function (field){
                var fieldName = field.getAttribute('id').replace('searchField-', ''),
                 appo = {};
                    if(field.value  !== ""){
                        appo["key"] = fieldName;
                        appo["value"] = field.value;
                        appo["type"] = field.getAttribute("type");
                        if(field.id.includes("initiator") || field.id.includes("titoloIstanzaFlusso")){
                            processParams.push(appo);
                        } else {
                            taskParams.push(appo);
                        }
                    }
            });
            return {"processParams": processParams, "taskParams": taskParams};
        }

        $scope.showProcessInstances = function(requestedPage) {
        //se non ho ancora effettuato query carico entrambe le "viste" ("i miei tas" e " i task di gruppo"
            if ((vm.availableTasks.total === 0 && vm.myTasks.total === 0)) {
                $scope.loadMyTasks();
                $scope.loadAvailableTasks();
            } else {
                if (vm.activeContent == 'myTasks') {
                    $scope.loadMyTasks();
                } else if (vm.activeContent == 'availables') {
                    $scope.loadAvailableTasks();
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
