(function() {
  "use strict";

  angular
    .module('sprintApp')
    .controller('ReassignModalController', ReassignModalController);

  ReassignModalController.$inject = ['$uibModalInstance','$scope', 'dataService','taskId', 'processInstanceId'];

  function ReassignModalController($uibModalInstance, $scope, dataService, taskId, processInstanceId) {
    var vm = this;

    vm.taskId = taskId;
    vm.processInstanceId = processInstanceId;

    $scope.reassignTask = function(user) {
      dataService.tasks.reassign(vm.taskId, vm.processInstanceId, vm.user).then(function(response) {
        if(response){
          $uibModalInstance.dismiss('cancel');
        }
      });
    };
  }
})();
