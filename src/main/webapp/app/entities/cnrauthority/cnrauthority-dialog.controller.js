(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrauthorityDialogController', CnrauthorityDialogController);

    CnrauthorityDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', '$q', 'entity', 'Cnrauthority', 'Authority'];

    function CnrauthorityDialogController ($timeout, $scope, $stateParams, $uibModalInstance, $q, entity, Cnrauthority, Authority) {
        var vm = this;

        vm.cnrauthority = entity;
        vm.clear = clear;
        vm.save = save;
        vm.authorities = Authority.query({filter: 'cnrauthority-is-null'});
        $q.all([vm.cnrauthority.$promise, vm.authorities.$promise]).then(function() {
            if (!vm.cnrauthority.authority || !vm.cnrauthority.authority.id) {
                return $q.reject();
            }
            return Authority.get({id : vm.cnrauthority.authority.id}).$promise;
        }).then(function(authority) {
            vm.authorities.push(authority);
        });

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
