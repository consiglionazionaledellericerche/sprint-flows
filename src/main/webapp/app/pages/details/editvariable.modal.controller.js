(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('EditVariableModalController', EditVariableModalController);


    EditVariableModalController.$inject = ['$uibModalInstance', 'dataService', 'processInstanceId', 'variableName', 'currentValue'];

    function EditVariableModalController ($uibModalInstance, dataService, processInstanceId, variableName, currentValue) {

      var vm = this;

      vm.processInstanceId = processInstanceId;
      vm.variableName = variableName;
      vm.currentValue = currentValue;

      vm.setVariable = function() {
        dataService.processInstances.setVariable(vm.processInstanceId, vm.variableName, vm.currentValue)
        .then(function() {
          $uibModalInstance.dismiss('ok');
        }, function() {
          vm.error = true;
        })
      }

    }
})();