(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('SwitchUserController', SwitchUserController);

    SwitchUserController.$inject = ['$rootScope', '$state', '$timeout', 'dataService', '$uibModalInstance', '$window', 'Principal'];

    function SwitchUserController ($rootScope, $state, $timeout, dataService, $uibModalInstance, $window, Principal) {
        var vm = this;

        vm.impersonationError = false;
        vm.cancel = cancel;
        vm.credentials = {};
        vm.switchUser = switchUser;
        vm.username = null;

        $timeout(function (){angular.element('#username').focus();});

        function cancel () {
            vm.credentials = {
                username: null
            };
            vm.impersonationError = false;
            $uibModalInstance.dismiss('cancel');
        }

        function switchUser (event) {
            event.preventDefault();

            dataService.authentication.impersonate(vm.username).then(function () {
                vm.impersonationError = false;
                $uibModalInstance.close();

                $rootScope.$broadcast('impersonationSuccess');
                Principal.authenticate(null);
                Principal.identity(true).then(function(account) {
                    $state.go('home');
                });
            }, function(response) {
                vm.impersonationError = true;
            })
        }
    }
})();
