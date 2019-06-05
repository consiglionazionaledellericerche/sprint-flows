(function() {
	'use strict';

	angular
	.module('sprintApp')
	.controller('StatisticsController', StatisticsController);

	StatisticsController.$inject = ['$rootScope', '$scope', '$filter', '$stateParams', 'dataService', 'Principal', 'ProfileService', 'LoginService', 'utils'];


	function StatisticsController($rootScope, $scope, $filter, $stateParams, dataService, Principal, ProfileService, LoginService, utils) {
		//var vm = this;


		var vm = this,
		dateFormat = 'yyyy-MM-dd';
		vm.processDefinition = $stateParams.processDefinition.split(":")[0];
		vm.isNavbarCollapsed = true;
		vm.isAuthenticated = Principal.isAuthenticated;

		$scope.exportFile = function(isPdf, processDefinitionKey, idStruttura, startDateGreat, startDateLess, filename) {
			var url = (isPdf ? '/api/makeStatisticPdf?' : '/api/makeStatisticCsv?') +
			'processDefinitionKey=' + vm.processDefinition +
			'&idStruttura=' + vm.exportParams.struttura +
			'&startDateGreat=' + $filter('date')(vm.exportParams.startDateGreat, dateFormat) +
			'&startDateLess=' + $filter('date')(vm.exportParams.startDateLess, dateFormat);
			utils.downloadFile(url, filename, isPdf ? 'application/pdf' : 'application/vnd.ms-excel');
		};

		// $scope.strutture = [{id:1,label:"pippo"},{id:2,label:"pluto"}];
		$scope.strutture = [];
		var appoStruttura = [];
		function elencaStrutture() {
			for (var i = 0; i < Principal.identity().$$state.value.authorities.length; i++){
				var authority = Principal.identity().$$state.value.authorities[i];
				if(authority.includes('responsabile#' + vm.processDefinition) || authority.includes('supervisore#' + vm.processDefinition)){
					var newStruttura = authority.split(/[#@]/)[2];
					if(newStruttura != null){
						if(newStruttura == "0000"){
							appoStruttura.push({
								value: newStruttura,
								label: "CNR"
							})
							$scope.strutture.push({
								value: newStruttura,
								label: "CNR"
							})
						} else {
							if(appoStruttura.indexOf(newStruttura) == -1) {
								appoStruttura.push(newStruttura);
								dataService.lookup.uo(newStruttura).then(function(response){
									$scope.strutture.push(response.data)
								})
							}
						}
					}
				}
			}
		};

		elencaStrutture();
	}
})();
