(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrgroupDeleteController',CnrgroupDeleteController);

    CnrgroupDeleteController.$inject = ['$uibModalInstance', 'entity', 'Cnrgroup'];

    function CnrgroupDeleteController($uibModalInstance, entity, Cnrgroup) {
        var vm = this;

        vm.cnrgroup = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Cnrgroup.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
