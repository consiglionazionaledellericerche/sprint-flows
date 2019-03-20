(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ExternalMessageDialogController', ExternalMessageDialogController);

    ExternalMessageDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'ExternalMessage'];

    function ExternalMessageDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, ExternalMessage) {
        var vm = this;

        vm.externalMessage = entity;
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
            if (vm.externalMessage.id !== null) {
                ExternalMessage.update(vm.externalMessage, onSaveSuccess, onSaveError);
            } else {
                ExternalMessage.save(vm.externalMessage, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:externalMessageUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
