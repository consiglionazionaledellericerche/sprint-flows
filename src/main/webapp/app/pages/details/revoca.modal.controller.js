(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('RevocaModalController', RevocaModalController);


    RevocaModalController.$inject = ['$uibModalInstance', 'dataService', 'processInstanceId'];

    function RevocaModalController ($uibModalInstance, dataService, processInstanceId) {

      var vm = this;

      vm.processInstanceId = processInstanceId;

      vm.revoca = function() {
        dataService.processInstances.revoca(vm.processInstanceId)
        .then(function() {
          $uibModalInstance.dismiss('ok');
        }, function() {
          vm.error = true;
        })
      }

    }
})();