(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('TaskController', HomeController);

  HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', '$log'];

  function HomeController ($scope, Principal, LoginService, $state, dataService, $log) {
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
        dataService.tasks.complete(vm.data);
      }
    }

    function validate(data) {
      $log.debug("validation not implemented yet");
      return true;
    }
  }
})();
