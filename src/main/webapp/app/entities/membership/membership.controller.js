(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('MembershipController', MembershipController);

	MembershipController.$inject = ['$scope', '$state', 'Membership', 'ParseLinks', 'AlertService'];

	function MembershipController($scope, $state, Membership, ParseLinks, AlertService) {

		$scope.memberships = [];
		$scope.predicate = 'id';
		$scope.reverse = true;
		$scope.page = 1;
		$scope.loadAll = function() {
			Membership.query({
				page: $scope.page - 1,
				size: 20,
				sort: [$scope.predicate + ',' + ($scope.reverse ? 'asc' : 'desc'), 'id']
			}, function(result, headers) {
				$scope.links = ParseLinks.parse(headers('link'));
				$scope.totalItems = headers('X-Total-Count');
				$scope.memberships = result;
			});
		};
		$scope.loadPage = function(page) {
			$scope.page = page;
			$scope.loadAll();
		};
		$scope.loadAll();


		$scope.refresh = function() {
			$scope.loadAll();
			$scope.clear();
		};

		$scope.clear = function() {
			$scope.membership = {
				grouprole: null,
				id: null
			};
		};
	}
})();