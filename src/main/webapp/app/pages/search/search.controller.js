(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('SearchController', SearchController);

    SearchController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'AlertService', 'paginationConstants', 'utils', '$log'];

    function SearchController($scope, $rootScope, $state, dataService, AlertService, paginationConstants, utils, $log) {
        var vm = this;

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

        $log.info($state.params.processDefinition);

        // Reload ricerca in caso di modifica dell'ordine di visualizzazione (crescente/decrescente)
        $scope.$watchGroup(['vm.order'], function() {
            $scope.search();
        });


        $scope.showProcessInstances = function(active) {
            vm.active = active;
            $scope.search();
        };


        $scope.search = function() {
            var fields = Array.from($("input[id^='searchField-']")),
                params = [],
                firstResult,
                maxResults = vm.itemsPerPage;
            firstResult = vm.itemsPerPage * (vm.page - 1)

            //popolo params con gli id, i valori sottomessi e il "type" dei campi di ricerca
            fields.forEach(function(field) {
                var fieldName = field.getAttribute('id').replace('searchField-', ''),
                    appo = {};
                if (field.value !== "") {
                    appo["key"] = fieldName;
                    appo["value"] = field.value;
                    appo["type"] = field.getAttribute("type");
                    params.push(appo);
                }
            });
            var paramsJson = {
                "params": params
            };

            dataService.processInstances.search($scope.current, vm.active, paramsJson, vm.order, firstResult, maxResults)
                .then(function(response) {
                    vm.processInstances = utils.refactoringVariables(response.data.processInstances);
                    // variabili per la gestione della paginazione
                    vm.totalItems = response.data.totalItems;
                    vm.queryCount = vm.totalItems;
                }, function(response) {
                    $log.error(response);
                });
        }
            //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        function transition() {
            $scope.search();
        }
    }
})();