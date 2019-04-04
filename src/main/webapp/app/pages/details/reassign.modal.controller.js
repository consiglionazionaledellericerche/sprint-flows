(function() {
  "use strict";

  angular
    .module("sprintApp")
    .controller("ReassignModalController", ReassignModalController);

  ReassignModalController.$inject = ["$uibModalInstance", "$scope", "dataService", "taskId", "processInstanceId", "$state"];

  function ReassignModalController($uibModalInstance, $scope, dataService, taskId, processInstanceId, $state) {
    var vm = this;

    vm.taskId = taskId;
    vm.processInstanceId = processInstanceId;

    $scope.reassignTask = function(user) {
      dataService.tasks.reassign(vm.taskId, vm.processInstanceId, vm.user)
        .then(function(response) {
          if (response) {
            $uibModalInstance.dismiss("cancel");
            $state.reload();
          }
        });
    };
  }
})();
