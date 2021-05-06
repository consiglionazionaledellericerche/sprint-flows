(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('BlacklistDeleteController',BlacklistDeleteController);

    BlacklistDeleteController.$inject = ['$uibModalInstance', 'entity', 'Blacklist'];

    function BlacklistDeleteController($uibModalInstance, entity, Blacklist) {
        var vm = this;

        vm.blacklist = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Blacklist.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
