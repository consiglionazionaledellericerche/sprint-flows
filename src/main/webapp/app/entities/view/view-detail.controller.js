(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ViewDetailController', ViewDetailController);

    ViewDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'View'];

    function ViewDetailController($scope, $rootScope, $stateParams, previousState, entity, View) {
        var vm = this;

        vm.view = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:viewUpdate', function(event, result) {
            vm.view = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
