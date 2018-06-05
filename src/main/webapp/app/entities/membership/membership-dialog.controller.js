(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('MembershipDialogController', MembershipDialogController);

    MembershipDialogController.$inject = ['$scope', 'dataService', '$uibModalInstance'];

    function MembershipDialogController($scope, dataService, $uibModalInstance) {
        $scope.save = function() {
            $scope.isSaving = true;

            dataService.userMemberships.createMembership($scope.membership.cnrgroup, $scope.membership.username, $scope.membership.grouprole)
                .then(function onSaveSuccess(result) {
                    $scope.$emit('sprintApp:membershipUpdate', result);
                    $uibModalInstance.close(result);
                    vm.isSaving = false;
                }, function onSaveError() {
                    vm.isSaving = false;
                });
        };

        $scope.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
    }
})();
