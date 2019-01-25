(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('AvvisoDialogController', AvvisoDialogController);

    AvvisoDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Avviso'];

    function AvvisoDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Avviso) {
        var vm = this;

        vm.avviso = entity;
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
            if (vm.avviso.id !== null) {
                Avviso.update(vm.avviso, onSaveSuccess, onSaveError);
            } else {
                Avviso.save(vm.avviso, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:avvisoUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
