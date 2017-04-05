(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('MyProcessInstancesController', MyProcessInstancesController);

  MyProcessInstancesController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', '$log'];

  function MyProcessInstancesController ($scope, Principal, LoginService, $state, dataService, $log) {
    var vm = this;
    vm.myTasks = {total: 0};
    vm.pooledTasks = {total: 0};

    $scope.loadMyProcessInstances = function() {
      dataService.processInstances.myProcessInstances(true)
      .then(function (response) {
          vm.myProcessInstancesActive = response.data;
        }, function (error) {
          $log.error(error);
        });

      dataService.processInstances.myProcessInstances(false)
      .then(function (response) {
          vm.myProcessInstancesTerminated = response.data;
        }, function (response) {
          $log.error(response);
        });
    }

    $scope.loadMyProcessInstances();

    $scope.setActiveContent = function(choice) {
        $scope.activeContent = choice;
    }
  }
})();
