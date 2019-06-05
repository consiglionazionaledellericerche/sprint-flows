(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('UserMembershipNewController', UserMembershipNewController);

	UserMembershipNewController.$inject = ['$scope', 'dataService', '$uibModalInstance', 'entity', 'Membership'];

	function UserMembershipNewController($scope, dataService , $uibModalInstance, entity, Membership) {
		var vm = this;

		vm.membership = entity;
		vm.clear = clear;
		vm.save = save;

		function clear() {
			$uibModalInstance.dismiss('cancel');
		}

		function save() {
			vm.isSaving = true;
			dataService.userMemberships.createMembership(vm.membership.groupname, vm.membership.username, vm.membership.grouprole)
				.then(function onSaveSuccess(result) {
					$scope.$emit('sprintApp:membershipUpdate', result);
					$uibModalInstance.close(result);
					vm.isSaving = false;
				}, function onSaveError() {
					vm.isSaving = false;
				});
		}
	}
})();