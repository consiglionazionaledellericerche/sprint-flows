(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', 'ProfileService', '$q', '$state', 'dataService'];

    function HomeController ($scope, Principal, LoginService, ProfileService, $q, $state, dataService) {
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
            var principalPromise = Principal.identity()
            var profilePromise   = ProfileService.getProfileInfo();

            $q.all([principalPromise, profilePromise]).then(function(data) {
                vm.account = data[0];
                vm.profiles = data[1].activeProfiles;
                vm.isAuthenticated = Principal.isAuthenticated;
                if ( vm.isAuthenticated && vm.profiles.includes('cnr') )
                    getTasksCount();
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
