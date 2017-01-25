(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('TaskController', HomeController);

  HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log'];

  function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log) {
    var vm = this;
    vm.data = {};
    vm.data.taskId = $state.params.taskId;

    vm.formUrl = 'app/forms/'+ $state.params.processDefinition +'/'+ $state.params.taskName +'.html'
    $scope.data = {};

    dataService.definitions.get($state.params.processDefinition)
    .then(
        function(response) {
          vm.definition = response.data;
          vm.data.definitionId = vm.definition.id;
        },
        function(response) {
          $log.error(response);
        }
    );

    $scope.submitTask = function() {

      $log.info(vm);
      if (validate(vm.data)) {
        dataService.tasks.complete(vm.data)
          .then(
            function(data) {
              $log.info(data);
              AlertService.success("Task creato con successo");
              $state.go('availabletasks');
            },
            function(err) {
              $log.error(data);
              AlertService.error("Creazione task non riuscita");
            });
      }
    }

    function validate(data) {
      $log.debug("validation not implemented yet");
      return true;
    }
  }
})();
