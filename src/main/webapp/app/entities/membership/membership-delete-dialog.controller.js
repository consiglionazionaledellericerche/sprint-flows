(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('MembershipDialogController', MembershipDialogController);

	MembershipDialogController.$inject = ['$timeout', '$scope', '$uibModalInstance', 'entity', 'Membership'];

	function MembershipDialogController($timeout, $scope, $uibModalInstance, entity, Membership) {

		$scope.membership = entity;
		$scope.clear = function() {
			$uibModalInstance.dismiss('cancel');
		};
		$scope.confirmDelete = function(id) {
			Membership.delete({
					id: id
				},
				function() {
					$uibModalInstance.close(true);
				});
		};
	}
})();