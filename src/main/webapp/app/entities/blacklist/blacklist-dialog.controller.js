(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('BlacklistDialogController', BlacklistDialogController);

    BlacklistDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Blacklist'];

    function BlacklistDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Blacklist) {
        var vm = this;

        vm.blacklist = entity;
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
            if (vm.blacklist.id !== null) {
                Blacklist.update(vm.blacklist, onSaveSuccess, onSaveError);
            } else {
                Blacklist.save(vm.blacklist, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('flowsApp:blacklistUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
