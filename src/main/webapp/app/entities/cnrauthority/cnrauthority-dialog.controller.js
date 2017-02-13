(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrauthorityDialogController', CnrauthorityDialogController);

    CnrauthorityDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Cnrauthority'];

    function CnrauthorityDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Cnrauthority) {
        var vm = this;

        vm.cnrauthority = entity;
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
            if (vm.cnrauthority.id !== null) {
                Cnrauthority.update(vm.cnrauthority, onSaveSuccess, onSaveError);
            } else {
                Cnrauthority.save(vm.cnrauthority, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:cnrauthorityUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
