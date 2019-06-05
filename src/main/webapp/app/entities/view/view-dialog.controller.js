(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ViewDialogController', ViewDialogController);

    ViewDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'View'];

    function ViewDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, View) {
        var vm = this;

        vm.view = entity;
        vm.clear = clear;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.view.id !== null) {
                View.update(vm.view, onSaveSuccess, onSaveError);
            } else {
                View.save(vm.view, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:viewUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
