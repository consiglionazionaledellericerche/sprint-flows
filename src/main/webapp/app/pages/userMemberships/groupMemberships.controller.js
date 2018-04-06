(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('GroupMembershipsController', GroupMembershipsController);

	GroupMembershipsController.$inject = ['$scope', '$state', '$stateParams', 'Membership', 'dataService', 'ParseLinks', 'AlertService', 'pagingParams', 'paginationConstants', 'modify'];

	function GroupMembershipsController($scope, $state, $stateParams, Membership, dataService, ParseLinks, AlertService, pagingParams, paginationConstants, modify) {
		var vm = this;

		vm.modify = modify;
		vm.loadPage = loadPage;
		vm.predicate = pagingParams.predicate;
		vm.reverse = pagingParams.ascending;
		vm.transition = transition;
		vm.itemsPerPage = paginationConstants.itemsPerPage;
		vm.groupname = $stateParams.groupname;

		loadAll();


		function loadAll() {
			dataService.userMemberships.groupMembersByGroupName(vm.groupname).then(onSuccess, onError);

			function onSuccess(data) {
				vm.links = ParseLinks.parse(data.headers('link'));
				vm.totalItems = data.headers('X-Total-Count');
				vm.queryCount = vm.totalItems;
				vm.memberships = data.data;
				vm.page = pagingParams.page;
			}

			function onError(error) {
				AlertService.error(error.data.message);
			}
		}


		function loadPage(page) {
			vm.page = page;
			vm.transition();
		}


		function transition() {
			$state.transitionTo($state.$current, {
				page: vm.page,
				sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
				search: vm.currentSearch
			});
		}


		vm.removeUserMembership = function(id) {
			Membership.delete({
					id: id
				},
				function() {
					$uibModalInstance.close(true);
				});
		};
	}
})();