(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FormDetailController', FormDetailController);

    FormDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Form'];

    function FormDetailController($scope, $rootScope, $stateParams, previousState, entity, Form) {
        var vm = this;

        vm.form = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:formUpdate', function(event, result) {
            vm.form = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
