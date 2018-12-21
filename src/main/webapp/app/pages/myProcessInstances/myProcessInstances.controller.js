(function () {
	'use strict';

    angular
            .module('sprintApp')
            .controller('MyPIController', MyPIController);

    MyPIController.$inject = ['$scope', 'paginationConstants', 'dataService', 'utils', '$log'];


    function MyPIController($scope, paginationConstants, dataService, utils, $log) {
        var vm = this;
        vm.order = 'ASC';

        //variabili usate nella paginazione
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.pageActive = 1;
        vm.pageTerminated = 1;
        vm.totalItemsActive = vm.itemsPerPage * vm.pageActive;
        vm.totalItemsTerminated = vm.itemsPerPage * vm.pageTerminated;
        vm.searchParams = {};

        vm.myTasks = {total: 0};
        vm.pooledTasks = {total: 0};

        $scope.$watchGroup(['vm.order', 'current'], function () {
                $scope.loadMyProcessInstances();
        });

        $scope.loadMyProcessInstances = function () {
            var searchParamsActive = {},
                searchParamsTerminated = {};

            //carico le form di ricerca specifiche per ogni tipologia di Process Definitions
            $scope.formUrl = utils.loadSearchFields(vm.searchParams.processDefinitionKey, false);
            //setto il searchParams per la procesDefinitionKey
            vm.searchParams.processDefinitionKey = vm.processDefinitionKey ? vm.processDefinitionKey : 'all';
            //setto i searchParams per la query sui flussi attivi
            searchParamsActive = $.extend({}, vm.searchParams);
            searchParamsActive.page = vm.pageActive || 1;
            searchParamsActive.active = true;
            dataService.processInstances.myProcessInstances(searchParamsActive)
                    .then(function (response) {
                        vm.myProcessInstancesActive = utils.refactoringVariables(response.data.processInstances);
                        vm.totalItemsActive = response.data.totalItems;
                        vm.queryCountActive = vm.totalItemsActive;
                    }, function (error) {
                        $log.error(error);
                    });

            //setto i searchParams per la query sui flussi terminati
            searchParamsTerminated = $.extend({}, vm.searchParams);
            searchParamsTerminated.page = vm.TAIMGPage || 1;
            searchParamsTerminated.active = false;
            dataService.processInstances.myProcessInstances(searchParamsTerminated)
                    .then(function (response) {
                        vm.myProcessInstancesTerminated = utils.refactoringVariables(response.data.processInstances);
                        vm.totalItemsTerminated = response.data.totalItems;
                        vm.queryCountTerminated = vm.totalItemsTerminated;
                    }, function (response) {
                        $log.error(response);
                    });
        };

        //rimuove la div con lo user perchè questa è la pagina dei "MIEI" flussi
        $scope.removeUserDiv = function () {
             $("div").remove("#userDiv");
        };

        $scope.setActiveContent = function (choice) {
            $scope.activeContent = choice;
        };

        //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        vm.transition = function transition () {
            $scope.loadMyProcessInstances();
        };
    }
})();