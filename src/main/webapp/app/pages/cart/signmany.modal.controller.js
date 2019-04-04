(function() {
  "use strict";

  angular
  .module('sprintApp')
  .controller('SignManyModalController', SignManyModalController);

  SignManyModalController.$inject = ['$uibModalInstance','$scope', 'dataService', '$localStorage', '$state', 'AlertService'];

  function SignManyModalController($uibModalInstance, $scope, dataService, $localStorage, $state, AlertService) {
    var vm = this;


    vm.signMany = function() {
      vm.message = undefined;
      vm.submitDisabled = true;

      dataService.signMany(vm.username, vm.password, vm.otp, Object.keys($localStorage.cart)).then(
          function(response) {
            if (response.data.failure.length > 0) {
              var errors = response.data.failure.map(function (el) {return "<br>" +el.split(":")[1];});
              response.data.success.forEach(function (el) {
                delete $localStorage.cart[el];
              })

              vm.message      = "La firma di alcuni dei documenti non è andata a buon fine"+ errors;
              vm.messageClass = "alert-warning";
            } else {
              delete $localStorage.cart;
              $uibModalInstance.close();
              $state.go('availableTasks')
              AlertService.success("La firma di tutti i documenti è stata eseguita con successo");
            }
          },
          function(response) {
            vm.message = response.data.message;
            vm.messageClass = "alert-danger";
          }).finally(function () {
            vm.submitDisabled = false;
          });
    }
  }
})();
