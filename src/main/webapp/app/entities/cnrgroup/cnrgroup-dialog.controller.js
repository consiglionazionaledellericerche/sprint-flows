(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrgroupDialogController', CnrgroupDialogController);

    CnrgroupDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Cnrgroup', 'User'];

    function CnrgroupDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Cnrgroup, User) {
        var vm = this;

        vm.cnrgroup = entity;
        vm.clear = clear;
        vm.save = save;
        vm.cnrgroups = Cnrgroup.query();
        vm.users = User.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.cnrgroup.id !== null) {
                Cnrgroup.update(vm.cnrgroup, onSaveSuccess, onSaveError);
            } else {
                Cnrgroup.save(vm.cnrgroup, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:cnrgroupUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
