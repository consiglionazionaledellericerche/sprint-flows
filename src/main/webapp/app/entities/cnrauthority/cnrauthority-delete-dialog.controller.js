(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrauthorityDeleteController',CnrauthorityDeleteController);

    CnrauthorityDeleteController.$inject = ['$uibModalInstance', 'entity', 'Cnrauthority'];

    function CnrauthorityDeleteController($uibModalInstance, entity, Cnrauthority) {
        var vm = this;

        vm.cnrauthority = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Cnrauthority.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
