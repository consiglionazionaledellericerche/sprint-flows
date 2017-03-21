(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('SearchController', SearchController);

  SearchController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'AlertService', 'paginationConstants', '$log'];

  function SearchController ($scope, $rootScope, $state, dataService, AlertService, paginationConstants, $log) {
    var vm = this;

    vm.availableFilter = $rootScope.availableFilter;
    vm.order;
    vm.active = true;
    //serve per resettare la label della tipologia di Process Definition scelta in caso di passaggio "temporaneo" in un'altra pagina
    $rootScope.current = undefined;
    //variabili usate nella paginazione
    vm.itemsPerPage = paginationConstants.itemsPerPage;
    vm.transition = transition;
    vm.page = 1;
    vm.totalItems = vm.itemsPerPage * vm.page;

    $log.info($state.params.processDefinition);

    $scope.orderSearchFlows = function(processDefinition, order) {
        order === 'ASC' ? $('#order').text('Crescente') : $('#order').text('Decrescente');
        vm.order = order;
        if(processDefinition !== undefined){
            $scope.search(processDefinition);
        }
    };

    $scope.showWorkflows = function (processDefinition, active) {
        vm.active = active;
        $scope.search(processDefinition);
    };


    $scope.search = function(processDefinition){
        var fields = Array.from($("input[id^='searchFields']")), params = [], firstResult,
           maxResults = vm.itemsPerPage;
        firstResult = vm.itemsPerPage * (vm.page - 1)

        if(vm.order !== undefined && processDefinition !== undefined){
            //popolo params con gli id, i valori sottomessi e il "type" dei campi di ricerca
            fields.forEach(function (field){
                var fieldName = field.getAttribute('id').replace('searchFields.', ''), appo = {};
                    if(field.value  !== ""){
                        appo["key"] = fieldName;
                        appo["value"] = field.value;
                        appo["type"] = field.getAttribute("type");
                        params.push(appo);
                    }
            });
            var paramsJson = {"params": params};

            dataService.tasks.searchTask(processDefinition.key, vm.active, paramsJson, vm.order, firstResult, maxResults)
                .then(function (response) {
                    vm.tasks = response.data.tasks;
                    // variabili per la gestione della paginazione
                    vm.totalItems = response.data.totalItems;
                    vm.queryCount = vm.totalItems;
                }, function (response) {
                    $log.error(response);
                });
        } else {
            if(vm.order === undefined) {
                AlertService.warning("Scegliere un ordine in cui visualizzare i risultani della ricerca");
            } else if(processDefinition === undefined){
                AlertService.warning("Definire un Process Definition di cui ricercare le istanze");
            }
        }
    }

    //funzione richiamata quando si chiede una nuova "pagina"
    function transition (current) {
        $scope.search(current);
    }
  }
})();
