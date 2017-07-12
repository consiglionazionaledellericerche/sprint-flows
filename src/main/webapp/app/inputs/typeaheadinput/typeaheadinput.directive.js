(function() {
	'use strict';

	angular.module('sprintApp')
	.directive('typeaheadinput', typeaheadinput);

	typeaheadinput.$inject = ['dataService', '$log'];

	/**
	 * Questa direttiva e' un semplicissimo typeahead con dei parametri preimpostati
	 * L'API e' semplicissima: ng-model e ng-required
	 */
	function typeaheadinput(dataService, $log) {

		return {
			restrict: 'E',
			templateUrl: 'app/inputs/typeaheadinput/typeaheadinput.html',
			scope: {
				ngModel: '=',
				type: '@',
				ngRequired: '@'
			},
			link: function ($scope, element, attrs) {

				$scope.loadRecords = function(filter) {
					if ($scope.type === "users"){
						return dataService.search.users(filter)
						.then(function(response) {
							$scope.hasMore = response.data.more;
							return response.data.results;
						});
					} else if ($scope.type === "uo"){
						return dataService.search.uo(filter)
						.then(function(response) {
							$scope.hasMore = response.data.more;
							return response.data.results;
						});
					} else {
						$log.error("Type non riconosciuto " + $scope.type);
					}
				}
			}
		}
	}
})();