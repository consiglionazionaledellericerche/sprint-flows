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
        $scope.search(processDefinition);
    };

    $scope.showWorkflows = function (processDefinition, active) {
        vm.active = active;
        $scope.search(processDefinition);
    };




    $scope.search = function(processDefinition){

        var fields = Array.from($("input[id^='searchFields']")),
          params = [];

        if(vm.active !== undefined && vm.order !== undefined && processDefinition !== undefined){
            //popolo params con gli id dei campi ed i valori sottomessi nei campi di ricerca
            fields.forEach(function (field){
                var fieldName = field.getAttribute('id').replace('searchFields.', ''), appo = {};
                    if(field.value  !== ""){
                        appo[fieldName] = field.value;
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
//            todo: modale per definire l'ordine, i processi attivi e lo process definition?'
        }
    }
  }
})();
