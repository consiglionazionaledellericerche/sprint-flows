(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('UserMembershipDeleteController', UserMembershipDeleteController);

	UserMembershipDeleteController.$inject = ['$uibModalInstance', 'entity', 'Membership'];

	function UserMembershipDeleteController($uibModalInstance, entity, Membership) {
		var vm = this;

		vm.membership = entity;
		vm.clear = clear;
		vm.confirmDelete = confirmDelete;

		function clear() {
			$uibModalInstance.dismiss('cancel');
		}

		function confirmDelete(id) {
			Membership.delete({
					id: id
				},
				function() {
					$uibModalInstance.close(true);
				});
		}
	}
})();