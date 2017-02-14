(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'SwitchUserService', 'dataService', '$log'];

    function NavbarController ($scope, $state, Auth, Principal, ProfileService, LoginService, SwitchUserService, dataService, $log) {
        var vm = this;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;
        Principal.identity().then(function(account) {
            $log.info(account);
            vm.account = account;
        });

        Principal.hasAuthority("ROLE_PREVIOUS_ADMINISTRATOR").then(function(response) {
            vm.isImpersonating = response;
        });

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
              //$state.go('home');
              $window.location.reload();
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


    }
})();
