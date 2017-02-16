(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FormDeleteController',FormDeleteController);

    FormDeleteController.$inject = ['$uibModalInstance', 'entity', 'Form'];

    function FormDeleteController($uibModalInstance, entity, Form) {
        var vm = this;

        vm.form = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Form.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
