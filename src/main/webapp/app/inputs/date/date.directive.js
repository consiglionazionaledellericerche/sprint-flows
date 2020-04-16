(function() {
	'use strict';

	angular.module('sprintApp')
	.directive('date', date);

	date.$inject = ['dataService', '$log'];

	function date(dataService, $log) {
		return {
			restrict: 'E',
			templateUrl: 'app/inputs/date/date.html',
			scope: {
				ngModel: '=',
				ngRequired: '@'
			},
			link: function($scope, element, attrs) {

				$scope.isOpen = false;
				$scope.open = function() {
					$scope.isOpen = true;
				}
				
				// l'autofill non funziona automaticamente per componenti custom
				if ('autofill' in attrs) {
					var nomeModelId = attrs.ngModel.split('.').pop();
					$scope.ngModel = $scope.$parent.data.entity.variabili[nomeModelId];
				}
			}
		};
	}
})();
