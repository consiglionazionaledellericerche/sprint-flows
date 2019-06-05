(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('MembershipDetailController', MembershipDetailController);

	MembershipDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'entity', 'Membership'];

	function MembershipDetailController($scope, $rootScope, $stateParams, entity, Membership) {
		$scope.membership = entity;
		$scope.load = function(id) {
			Membership.get({
				id: id
			}, function(result) {
				$scope.membership = result;
			});
		};
		var unsubscribe = $rootScope.$on('sprintApp:membershipUpdate', function(event, result) {
			$scope.membership = result;
		});
		$scope.$on('$destroy', unsubscribe);
	}
})();