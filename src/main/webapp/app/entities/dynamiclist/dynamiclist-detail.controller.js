(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('DynamiclistDetailController', DynamiclistDetailController);

    DynamiclistDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Dynamiclist'];

    function DynamiclistDetailController($scope, $rootScope, $stateParams, previousState, entity, Dynamiclist) {
        var vm = this;

        vm.dynamiclist = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:dynamiclistUpdate', function(event, result) {
            vm.dynamiclist = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
