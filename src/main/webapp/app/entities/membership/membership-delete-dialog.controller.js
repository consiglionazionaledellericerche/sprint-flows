(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('MembershipDeleteController',MembershipDeleteController);

    MembershipDeleteController.$inject = ['$uibModalInstance', 'entity', 'Membership'];

    function MembershipDeleteController($uibModalInstance, entity, Membership) {
        var vm = this;

        vm.membership = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Membership.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
