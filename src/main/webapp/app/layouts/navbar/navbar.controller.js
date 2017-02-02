(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'dataService', '$log'];

    function NavbarController ($scope, $state, Auth, Principal, ProfileService, LoginService, dataService, $log) {
        var vm = this;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;

        ProfileService.getProfileInfo().then(function(response) {
            vm.inProduction = response.inProduction;
            vm.swaggerEnabled = response.swaggerEnabled;
        });

        vm.login = login;
        vm.logout = logout;
        vm.toggleNavbar = toggleNavbar;
        vm.collapseNavbar = collapseNavbar;
        vm.$state = $state;

        function login() {
            collapseNavbar();
            LoginService.open();
        }

        function logout() {
            collapseNavbar();
            Auth.logout();
            $state.go('home');
            vm.wfDefs = [];
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
