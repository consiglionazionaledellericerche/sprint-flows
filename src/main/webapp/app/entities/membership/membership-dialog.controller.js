(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('MembershipDialogController', MembershipDialogController);

	MembershipDialogController.$inject = ['$scope', '$stateParams', '$uibModalInstance', 'entity', 'Membership', 'Cnrgroup', 'Jhi_user'];

	function MembershipDialogController($scope, $stateParams, $uibModalInstance, entity, Membership, Cnrgroup, Jhi_user) {
		$scope.membership = entity;
		$scope.cnrgroups = Cnrgroup.query();
		$scope.jhi_users = Jhi_user.query();
		$scope.load = function(id) {
			Membership.get({
				id: id
			}, function(result) {
				$scope.membership = result;
			});
		};

		var onSaveSuccess = function(result) {
			$scope.$emit('sprintApp:membershipUpdate', result);
			$uibModalInstance.close(result);
			$scope.isSaving = false;
		};

		var onSaveError = function(result) {
			$scope.isSaving = false;
		};

		$scope.save = function() {
			$scope.isSaving = true;
			if ($scope.membership.id != null) {
				Membership.update($scope.membership, onSaveSuccess, onSaveError);
			} else {
				Membership.save($scope.membership, onSaveSuccess, onSaveError);
			}
		};

		$scope.clear = function() {
			$uibModalInstance.dismiss('cancel');
		};
	}
})();