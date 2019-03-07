(function() {
  "use strict";

  angular
    .module('sprintApp')
    .controller('DeleteProcessModalController', DeleteProcessModalController);

  DeleteProcessModalController.$inject = ['$uibModalInstance','$scope', 'dataService', 'processInstanceId'];

  function DeleteProcessModalController($uibModalInstance, $scope, dataService, processInstanceId) {
    var vm = this;

    vm.processInstanceId = processInstanceId;

    $scope.deleteProcess = function(deleteReason) {
      dataService.processInstances.deleteProcessInstance(vm.processInstanceId, vm.deleteReason).then(function(response) {
        if(response){
          $uibModalInstance.dismiss('cancel');
        }
      });
    };
  }
})();
