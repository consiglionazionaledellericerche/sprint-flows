(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('RelationshipDialogController', RelationshipDialogController);

    RelationshipDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Relationship'];

    function RelationshipDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Relationship) {
        var vm = this;

        vm.relationship = entity;
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
            if (vm.relationship.id !== null) {
                Relationship.update(vm.relationship, onSaveSuccess, onSaveError);
            } else {
                Relationship.save(vm.relationship, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:relationshipUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
