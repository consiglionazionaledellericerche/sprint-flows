(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('AvailableTasksController', AvalableTasksController);

  AvalableTasksController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'utils', '$log'];

  function AvalableTasksController ($scope, Principal, LoginService, $state, dataService, utils, $log) {
    var vm = this;
    vm.myTasks = {total: 0};
    vm.pooledTasks = {total: 0};

    $scope.loadTasks = function() {
      dataService.tasks.myTasksAvailable()
      .then(function (response) {
          utils.refactoringVariables(response.data.data);
          vm.pooledTasks = response.data;
        }, function (response) {
          $log.error(response);
        });
      dataService.tasks.myTasks()
      .then(function (response) {
          utils.refactoringVariables(response.data.data);
          vm.myTasks = response.data;
        }, function (response) {
          $log.error(response);
        });
    }

    $scope.loadTasks();

    $scope.setActiveContent = function(choice) {
        $scope.activeContent = choice;
    }
  }


})();
