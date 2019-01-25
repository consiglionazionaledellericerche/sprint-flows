(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('AvvisoDetailController', AvvisoDetailController);

    AvvisoDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Avviso'];

    function AvvisoDetailController($scope, $rootScope, $stateParams, previousState, entity, Avviso) {
        var vm = this;

        vm.avviso = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:avvisoUpdate', function(event, result) {
            vm.avviso = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
