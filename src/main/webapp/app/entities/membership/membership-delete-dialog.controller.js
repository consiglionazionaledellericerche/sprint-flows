(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('MembershipDeleteController', MembershipDeleteController);

    MembershipDeleteController.$inject = ['$timeout', '$scope', '$uibModalInstance', 'entity', 'Membership'];

    function MembershipDeleteController($timeout, $scope, $uibModalInstance, entity, Membership) {
        $scope.membership = entity;
        $scope.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
        $scope.confirmDelete = function(id) {
            Membership.delete({
                    id: id,
                },
                function() {
                    $uibModalInstance.close(true);
                });
        };
    }
})();
