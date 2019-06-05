(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('RelationshipDeleteController',RelationshipDeleteController);

    RelationshipDeleteController.$inject = ['$uibModalInstance', 'entity', 'Relationship'];

    function RelationshipDeleteController($uibModalInstance, entity, Relationship) {
        var vm = this;

        vm.relationship = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Relationship.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
