(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('TaskCompletedController', TaskCompletedController);

    TaskCompletedController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'utils', 'paginationConstants', '$log'];

    function TaskCompletedController($scope, $rootScope, $state, dataService, utils, paginationConstants, $log) {
        var vm = this;
         //variabili usate nella paginazione
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.page = 1;
        vm.totalItems = vm.itemsPerPage * vm.page;
        vm.order = 'ASC';

        $scope.loadTaskCompleted = function () {
            var  maxResults = vm.itemsPerPage,
                firstResult = vm.itemsPerPage * (vm.page - 1),
                fields = Array.from($("input[id^='searchField-']")), processParams = [], taskParams = [];

            //popolo params con gli id, i valori sottomessi e il "type" dei campi di ricerca
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
            var paramsJson = {"processParams": processParams, "taskParams": taskParams};

            dataService.tasks.getTaskCompletedByMe($rootScope.current, firstResult, maxResults, vm.order, paramsJson)
                .then(function (response) {
                    response.data.data.forEach( function (task){
                        utils.refactoringVariables(task);
                    });
                    vm.taskCompletedForMe = response.data.data;
                    // variabili per la gestione della paginazione
                    vm.totalItems = response.data.total;
                    vm.queryCount = vm.totalItems;
                }, function (error) {
                    $log.error(error);
                });
        };
        //aggiornamento pagina in caso di cambio "ordinamento" o Process definition
        $scope.$watchGroup(['vm.order', 'current'], function () {
            $scope.loadTaskCompleted();
        });

        //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        vm.transition = function transition () {
            $scope.loadTaskCompleted()
        }
    }
})();
