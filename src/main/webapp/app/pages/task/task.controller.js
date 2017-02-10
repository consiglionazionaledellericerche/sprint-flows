(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('TaskController', HomeController);

  HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log'];

  function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log) {
    var vm = this;
    vm.data = {};

    $log.info($state.params.processDefinition);

    if ($state.params.taskId) {
        $log.info("getting task ifno");

        vm.data.taskId = $state.params.taskId;
        dataService.tasks.getTask($state.params.taskId).then(
                function(data) {
                    vm.diagramUrl = '/runtime/process-instances/'+ data.data.processInstanceId +'/diagram';
                    var processDefinitionKey = data.data.processDefinitionId.split(":")[0]
                    vm.formUrl = 'app/forms/'+ processDefinitionKey +'/'+ data.data.taskDefinitionKey +'.html'
                });
    } else {
        vm.data.definitionId = $state.params.processDefinitionId;
        var processDefinitionKey = $state.params.processDefinitionId.split(":")[0];
        vm.diagramUrl = "/rest/diagram/" + $state.params.processDefinitionId;
        vm.formUrl = 'app/forms/'+ processDefinitionKey +'/'+ $state.params.taskName +'.html'
    }

    $scope.submitTask = function() {

      $log.info(vm);
      if (validate(vm.data)) {
        dataService.tasks.complete(vm.data)
          .then(
            function(data) {
              $log.info(data);
              AlertService.success("Richiesta completata con successo");
              $state.go('availabletasks');
            },
            function(err) {
              $log.error(data);
              AlertService.error("Richiesta non riuscita");
            });
      }
    }

    function validate(data) {
      $log.debug("validation not implemented yet");
      return true;
    }
  }
})();
