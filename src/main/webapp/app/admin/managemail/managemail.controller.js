(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('ManageMailController', ManageMailController);

  ManageMailController.$inject = ['$scope', 'paginationConstants', 'dataService', 'utils', '$log', 'Upload'];

  function ManageMailController($scope, paginationConstants, dataService, utils, $log, Upload) {
    var vm = this;

    dataService.mail.isActive().then(function(response) {
      vm.active = response.data;
      $scope.$watch('vm.active', function(newValue) {
        dataService.mail.setActive(newValue);
      });
    });

    dataService.mail.getUsers().then(function(response) {
      vm.users = response.data;
    });

    $scope.setUsers = function() {
      dataService.mail.setUsers(vm.users);
    }




  }
})();