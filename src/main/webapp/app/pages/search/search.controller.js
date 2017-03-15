(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('SearchController', SearchController);

  SearchController.$inject = ['$scope', '$rootScope', 'LoginService', '$state', 'dataService', 'AlertService', '$log'];

  function SearchController ($scope, $rootScope, LoginService, $state, dataService, AlertService, $log) {
    var vm = this;

    vm.wfDefs = $rootScope.wfDefs;
    vm.availableFilter = $rootScope.availableFilter;
    vm.order;
    vm.active = true;


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

        var fields = Array.from($("input[id^='searchFields']")),
          params = [];

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

            dataService.tasks.searchTask(processDefinition.key, vm.active, paramsJson, vm.order)
            .then(function (response) {
                vm.tasks = response.data
            }, function (response) {
                $log.error(response);
            });

        } else {
            if(vm.order === undefined) {
                AlertService.error("Definire se visualizzare i risultani in ordine \"Crescente\" o \"Decrescente\"");
            } else if(processDefinition === undefined){
                AlertService.error("Definire un Process Definition di cui ricercare le istanze");
            }
        }
    }
  }
})();
