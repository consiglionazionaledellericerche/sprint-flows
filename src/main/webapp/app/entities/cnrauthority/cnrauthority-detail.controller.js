(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrauthorityDetailController', CnrauthorityDetailController);

    CnrauthorityDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Cnrauthority'];

    function CnrauthorityDetailController($scope, $rootScope, $stateParams, previousState, entity, Cnrauthority) {
        var vm = this;

        vm.cnrauthority = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:cnrauthorityUpdate', function(event, result) {
            vm.cnrauthority = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
