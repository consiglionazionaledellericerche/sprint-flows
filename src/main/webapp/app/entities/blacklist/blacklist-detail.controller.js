(function() {
    'use strict';

    angular
        .module('flowsApp')
        .controller('BlacklistDetailController', BlacklistDetailController);

    BlacklistDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Blacklist'];

    function BlacklistDetailController($scope, $rootScope, $stateParams, previousState, entity, Blacklist) {
        var vm = this;

        vm.blacklist = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('flowsApp:blacklistUpdate', function(event, result) {
            vm.blacklist = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
