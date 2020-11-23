(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', 'ProfileService', '$q', '$state', 'dataService', 'AuthServerProvider', '$rootScope'];

    function HomeController ($scope, Principal, LoginService, ProfileService, $q, $state, dataService, AuthServerProvider, $rootScope) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        dataService.avvisi.getAttivi().then(function(response) {
            vm.avvisi = response.data;
        })

        // Duplicato da login.controller.js per l'emergenza Covid19 
        // TODO non duplicare codice, spostare in un service
        // TODO: prima passava da auth.service.js(login()) che poi richiamava auth.oauth2.service.js
        vm.signin = function(event) {
            event.preventDefault();
            vm.authenticationError = false;
            AuthServerProvider.login({
                username: vm.username.toLowerCase(),
                password: vm.password,
                rememberMe: vm.rememberMe
            }).then(function () {
                $state.go('availableTasks');
                $rootScope.$broadcast('authenticationSuccess');
            }).catch(function () {
                vm.authenticationError = true;
            });
        }
        
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
