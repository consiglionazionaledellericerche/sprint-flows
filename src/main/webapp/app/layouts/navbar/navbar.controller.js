(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$rootScope', '$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'SwitchUserService', 'dataService', '$log'];

    function NavbarController($rootScope, $scope, $state, Auth, Principal, ProfileService, LoginService, SwitchUserService, dataService, $log) {
        var vm = this, appo = true;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;

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
                    $state.reload();
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
            $rootScope.wfDefs = []; // TODO la logica e' che gli oggetti non vanno svuotati qui
        }

        function toggleNavbar() {
            vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
        }

        function collapseNavbar() {
            vm.isNavbarCollapsed = true;
        }

        function loadAvailableDefinitions() {
            ProfileService.getProfileInfo().then(function(response) {
                vm.inProduction = response.inProduction;
                vm.swaggerEnabled = response.swaggerEnabled;
                //verifico qual è il profilo spring con cui è stata avviata l'app per caricare il corrispondente banner
                $rootScope.app = response.activeProfiles.includes('oiv') ? 'oiv' : 'cnr'
            });

            dataService.definitions.all()
                .then(function(response) {
                    $rootScope.wfDefs = response.data.data;
                    $rootScope.wfDefsAll = [];
                    $rootScope.wfDefsAll.push.apply($rootScope.wfDefsAll, response.data.data);
                    $rootScope.wfDefsAll.push({
                        key: "all",
                        name: "ALL"
                    });
                }, function(response) {
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