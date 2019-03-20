(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ExternalMessageDetailController', ExternalMessageDetailController);

    ExternalMessageDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'ExternalMessage'];

    function ExternalMessageDetailController($scope, $rootScope, $stateParams, previousState, entity, ExternalMessage) {
        var vm = this;

        vm.externalMessage = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:externalMessageUpdate', function(event, result) {
            vm.externalMessage = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
