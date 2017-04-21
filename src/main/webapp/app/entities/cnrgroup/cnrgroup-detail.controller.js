(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrgroupDetailController', CnrgroupDetailController);

    CnrgroupDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Cnrgroup'];

    function CnrgroupDetailController($scope, $rootScope, $stateParams, previousState, entity, Cnrgroup) {
        var vm = this;

        vm.cnrgroup = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:cnrgroupUpdate', function(event, result) {
            vm.cnrgroup = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
