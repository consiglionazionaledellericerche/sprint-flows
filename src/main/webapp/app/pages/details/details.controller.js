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
                    vm.data.entity = utils.refactoringVariables([response.data.entity])[0];
                    vm.data.history = response.data.history;
                    vm.diagramUrl = '/rest/diagram/processInstance/'+ vm.data.entity.id;
                });
    }
  }
})();
