(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService'];

    function HomeController ($scope, Principal, LoginService, $state, dataService) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        /* --- */

        function getTasksCount() {
            dataService.tasks.coolAvailableTasks().then(function(response) {
                vm.coolTasks = response.data;
            });
        }

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
                if (vm.isAuthenticated)
                    getTasksCount();
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
