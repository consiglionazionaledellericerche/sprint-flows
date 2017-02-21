(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('DetailsController', DetailsController);

  DetailsController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log'];

  function DetailsController ($scope, Principal, LoginService, $state, dataService, AlertService, $log) {
    var vm = this;
    vm.data = {};

    $log.info($state.params.processInstanceId);
    if ($state.params.processInstanceId) {
        $log.info("getting task info");

        vm.data.processInstanceId = $state.params.processInstanceId;
        dataService.processInstances.byProcessInstanceId($state.params.processInstanceId).then(
                function(response) {
//                    todo: la stessa cosa viene fatta in tasklist.directive.js: uniformare i metodi con uno di utilità
                    //sposto le properties dall'array variables(che cancello) in entity
                    response.data.entity.variables.map( function(el){
                       response.data.entity[el.name] = el.value;
                    })
                    delete response.data.entity.variables;
                    vm.data = response.data;
                });
    }
  }
})();
