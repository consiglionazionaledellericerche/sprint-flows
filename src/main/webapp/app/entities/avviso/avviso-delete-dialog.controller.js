(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('AvvisoDeleteController',AvvisoDeleteController);

    AvvisoDeleteController.$inject = ['$uibModalInstance', 'entity', 'Avviso'];

    function AvvisoDeleteController($uibModalInstance, entity, Avviso) {
        var vm = this;

        vm.avviso = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Avviso.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
