(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('StatisticsController', StatisticsController);

    StatisticsController.$inject = ['$scope', '$filter', '$stateParams', 'dataService', 'utils'];

    function StatisticsController($scope, $filter, $stateParams, dataService, utils) {
        let vm = this,
            dateFormat = 'yyyy-MM-dd';
   		vm.processDefinition = $stateParams.processDefinition;

		$scope.exportFile = function(isPdf, processDefinitionKey, startDateGreat, startDateLess, filename) {
			let url = (isPdf ? '/api/makeStatisticPdf?' : '/api/makeStatisticCsv?') +
			'processDefinitionKey=' + vm.processDefinition.key +
			 '&startDateGreat=' + $filter('date')(vm.exportParams.startDateGreat, dateFormat) +
			 '&startDateLess=' + $filter('date')(vm.exportParams.startDateLess, dateFormat);
            utils.downloadFile(url, filename, isPdf ? 'application/pdf' : 'application/vnd.ms-excel');
        };
    }
})();
