(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('DynamiclistDialogController', DynamiclistDialogController);

    DynamiclistDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Dynamiclist'];

    function DynamiclistDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Dynamiclist) {
        var vm = this;

        vm.dynamiclist = entity;
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
            if (vm.dynamiclist.id !== null) {
                Dynamiclist.update(vm.dynamiclist, onSaveSuccess, onSaveError);
            } else {
                Dynamiclist.save(vm.dynamiclist, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:dynamiclistUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
