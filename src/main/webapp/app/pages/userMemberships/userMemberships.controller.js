(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('UserMembershipsController', UserMembershipsController);

	UserMembershipsController.$inject = ['$scope', '$state', 'dataService', 'ParseLinks', 'AlertService', 'pagingParams', 'paginationConstants'];

	function UserMembershipsController($scope, $state, dataService, ParseLinks, AlertService, pagingParams, paginationConstants) {
		var vm = this;

		vm.loadPage = loadPage;
		vm.predicate = pagingParams.predicate;
		vm.reverse = pagingParams.ascending;
		vm.transition = transition;
		vm.itemsPerPage = paginationConstants.itemsPerPage;

		loadAll();

		function loadAll() {
			dataService.userMemberships.groupsForUser({
				page: pagingParams.page - 1,
				size: vm.itemsPerPage,
				sort: sort()
			}).then(onSuccess, onError);

			function sort() {
				var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
				if (vm.predicate !== 'id') {
					result.push('id');
				}
				return result;
			}

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
	}
})();