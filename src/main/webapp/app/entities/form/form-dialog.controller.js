(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FormDialogController', FormDialogController);

    FormDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Form'];

    function FormDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Form) {
        var vm = this;

        vm.form = entity;
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
            if (vm.form.id !== null) {
                Form.update(vm.form, onSaveSuccess, onSaveError);
            } else {
                Form.save(vm.form, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:formUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
