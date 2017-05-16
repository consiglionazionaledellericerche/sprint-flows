(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$rootScope', '$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'SwitchUserService', 'dataService', '$log'];

    function NavbarController ($rootScope, $scope, $state, Auth, Principal, ProfileService, LoginService, SwitchUserService, dataService, $log) {
        var vm = this;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;

        ProfileService.getProfileInfo().then(function(response) {
            vm.inProduction = response.inProduction;
            vm.swaggerEnabled = response.swaggerEnabled;
        });

        vm.login = login;
        vm.logout = logout;
        vm.switchUser = switchUser;
        vm.cancelSwitchUser = cancelSwitchUser;
        vm.toggleNavbar = toggleNavbar;
        vm.collapseNavbar = collapseNavbar;
        vm.$state = $state;

        function switchUser() {
            collapseNavbar();
            SwitchUserService.open();
        }

        function cancelSwitchUser() {
            collapseNavbar();
            dataService.authentication.cancelImpersonate().then(function() {
                Principal.authenticate(null);
                Principal.identity(true).then(function(account) {
                    $state.go('home');
                });
            })
        }

        function login() {
            collapseNavbar();
            LoginService.open();
        }

        function logout() {
            collapseNavbar();
            Auth.logout();
            $state.go('home');
            vm.wfDefs = []; // TODO la logica e' che gli oggetti non vanno svuotati qui
        }

        function toggleNavbar() {
            vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
        }

        function collapseNavbar() {
            vm.isNavbarCollapsed = true;
        }

        function loadAvailableDefinitions() {
            dataService.definitions.all()
            .then(function(response) {
                vm.wfDefs = response.data.data;
                $rootScope.wfDefs = response.data.data;
                $rootScope.wfDefs.push({key:"all", name: "ALL"});
            }, function (response) {
                $log.error(response);
            });
        }

        loadAvailableDefinitions();
        $scope.$on('authenticationSuccess', function(event, args) {
            $log.info(event);
            $log.info(args);
            loadAvailableDefinitions();
        });

        $scope.$watch(function() {
            return Principal.isAuthenticated();
        }, function() {
            Principal.identity().then(function(account) {
                vm.account = account;
            })
        });
    }
})();
