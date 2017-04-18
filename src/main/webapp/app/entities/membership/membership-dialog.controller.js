(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('MembershipDialogController', MembershipDialogController);

    MembershipDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Membership'];

    function MembershipDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Membership) {
        var vm = this;

        vm.membership = entity;
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
            if (vm.membership.id !== null) {
                Membership.update(vm.membership, onSaveSuccess, onSaveError);
            } else {
                Membership.save(vm.membership, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:membershipUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
