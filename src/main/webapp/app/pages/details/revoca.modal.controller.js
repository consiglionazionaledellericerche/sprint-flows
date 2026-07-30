(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('RevocaModalController', RevocaModalController);


    RevocaModalController.$inject = ['$uibModalInstance', 'dataService', 'processInstanceId', 'AlertService', '$state'];

    function RevocaModalController ($uibModalInstance, dataService, processInstanceId, AlertService, $state) {

      var vm = this;

      vm.processInstanceId = processInstanceId;

      vm.revoca = function() {
        dataService.processInstances.revoca(vm.processInstanceId)
        .then(function() {
          $uibModalInstance.dismiss('ok');
          AlertService.success("Richiesta completata con successo");
          $state.go('availableTasks');
        }, function(err) {
          console.log(err);
          AlertService.error("Richiesta non riuscita<br>" + err.data.message);
        })
      }

    }
})();