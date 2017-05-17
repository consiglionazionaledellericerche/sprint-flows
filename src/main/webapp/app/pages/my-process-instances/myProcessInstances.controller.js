(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('MyProcessInstancesController', MyProcessInstancesController);

    MyProcessInstancesController.$inject = ['$scope', '$rootScope', 'dataService', 'paginationConstants', '$log'];

    function MyProcessInstancesController($scope, $rootScope, dataService, paginationConstants, $log) {
        var vm = this;

        //variabili usate nella paginazione
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.pageActive = 1;
        vm.pageTerminated = 1;
        vm.totalItemsActive = vm.itemsPerPage * vm.pageActive;
        vm.totalItemsTerminated = vm.itemsPerPage * vm.pageTerminated;

        vm.myTasks = {total: 0};
        vm.pooledTasks = {total: 0};

        $scope.$watchGroup(['vm.order', 'current'], function () {
                $scope.loadMyProcessInstances();
        });

        $scope.loadMyProcessInstances = function () {
            var firstResultActive, firstResultTerminated,
                maxResultsActive = vm.itemsPerPage,
                maxResultsTerminated = vm.itemsPerPage;

            firstResultActive = vm.itemsPerPage * (vm.pageActive - 1);
            firstResultTerminated = vm.itemsPerPage * (vm.pageTerminated - 1);

            dataService.processInstances.myProcessInstances(true, $rootScope.current, vm.order, firstResultActive, maxResultsActive)
                    .then(function (response) {
                        vm.myProcessInstancesActive = response.data;
                        vm.totalItemsActive = response.data.total;
                        vm.queryCountActive = vm.totalItemsActive;
                    }, function (error) {
                        $log.error(error);
                    });

            dataService.processInstances.myProcessInstances(false, $rootScope.current, vm.order, firstResultTerminated, maxResultsTerminated)
                    .then(function (response) {
                        vm.myProcessInstancesTerminated = response.data;
                        vm.totalItemsTerminated = response.data.total;
                        vm.queryCountTerminated = vm.totalItemsTerminated;
                    }, function (response) {
                        $log.error(response);
                    });
        }

        $scope.setActiveContent = function (choice) {
            $scope.activeContent = choice;
        }

        //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        vm.transition = function transition () {
            $scope.loadMyProcessInstances();
        }
    }
})();
