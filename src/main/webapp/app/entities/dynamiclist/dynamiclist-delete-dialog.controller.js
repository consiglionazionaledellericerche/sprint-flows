(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('DynamiclistDeleteController',DynamiclistDeleteController);

    DynamiclistDeleteController.$inject = ['$uibModalInstance', 'entity', 'Dynamiclist'];

    function DynamiclistDeleteController($uibModalInstance, entity, Dynamiclist) {
        var vm = this;

        vm.dynamiclist = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Dynamiclist.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
