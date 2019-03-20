(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ExternalMessageDeleteController',ExternalMessageDeleteController);

    ExternalMessageDeleteController.$inject = ['$uibModalInstance', 'entity', 'ExternalMessage'];

    function ExternalMessageDeleteController($uibModalInstance, entity, ExternalMessage) {
        var vm = this;

        vm.externalMessage = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            ExternalMessage.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
