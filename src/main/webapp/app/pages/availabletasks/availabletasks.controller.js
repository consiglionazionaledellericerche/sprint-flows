(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('AvailableTasksController', HomeController);

  HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', '$log'];

  function HomeController ($scope, Principal, LoginService, $state, dataService, $log) {
    var vm = this;

    vm.account = null;
    vm.isAuthenticated = null;
    vm.login = LoginService.open;
    vm.register = register;
    $scope.$on('authenticationSuccess', function() {
      getAccount();
    });

    getAccount();

    function getAccount() {
      Principal.identity().then(function(account) {
        vm.account = account;
        vm.isAuthenticated = Principal.isAuthenticated;
      });
    }
    function register () {
      $state.go('register');
    }

    dataService.tasks.myTasksAvailable()
    .then(function (response) {
        vm.pooledTask = response.data.total;
      }, function (response) {
        $log.error(response);
      });

    dataService.tasks.myTasks()
    .then(function (response) {
        vm.summary = {
            'total' : response.data.total
        };
      }, function (response) {
        $log.error(response);
      });

  }
})();
