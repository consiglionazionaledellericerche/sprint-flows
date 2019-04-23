(function() {
	'use strict';

	angular.module('sprintApp')
		.directive('taskDetails', taskDetailsDirective);

	taskDetailsDirective.$inject = ['dataService', '$sessionStorage', '$uibModal', '$log'];

	function taskDetailsDirective(dataService, $sessionStorage, $uibModal, $log) {

		return {
			restrict: 'E',
			scope: {
				variables: '=',
				title: '='
			},
			templateUrl: 'app/components/task-details/task-details.html',
			link: function(scope) {

				scope.modalTaskMetadata = function(variables, title) {
					$log.info(variables);
					$uibModal.open({
						templateUrl: 'app/components/task-details/detailModal.html',
						controllerAs: 'vm',
						controller: 'DetailModalController',
						size: 'md',
						resolve: {
							variables: function() {
								//escludo le variabili di tipo "_json"
								return variables.filter(function(variable) { return !variable.name.includes("_json");});
							},
							title: function() {
								return title;
							}
						}
					});
				};
			}
		};
	}
})();