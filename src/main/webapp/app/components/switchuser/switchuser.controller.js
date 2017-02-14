(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('SwitchUserController', SwitchUserController);

    SwitchUserController.$inject = ['$rootScope', '$state', '$timeout', 'dataService', '$uibModalInstance', '$window'];

    function SwitchUserController ($rootScope, $state, $timeout, dataService, $uibModalInstance, $window) {
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

//                $state.go('home');
                $rootScope.$broadcast('impersonationSuccess');
                $window.location.reload();
                $state.reload();
            }, function(response) {
                vm.impersonationError = true;
            })
        }
    }
})();
