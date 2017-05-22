(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('TaskCompletedController', TaskCompletedController);

    TaskCompletedController.$inject = ['$scope', '$rootScope', '$state', 'dataService', 'utils', 'paginationConstants', '$log'];

    function TaskCompletedController($scope, $rootScope, $state, dataService, utils, paginationConstants, $log) {
        var vm = this, firstResult, maxResults, loadTaskCompleted;
         //variabili usate nella paginazione
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.page = 1;
        vm.totalItems = vm.itemsPerPage * vm.page;
        vm.order = 'ASC';

        $scope.$watchGroup(['vm.order', 'current'], function () {
                loadTaskCompleted();
        });

        loadTaskCompleted = function () {
            var  maxResults = vm.itemsPerPage,
                firstResult = vm.itemsPerPage * (vm.page - 1);
            dataService.tasks.getTaskCompletedByMe($rootScope.current, firstResult, maxResults, vm.order)
                .then(function (response) {
                    response.data.data.forEach( function (task){
                        utils.refactoringVariables(task);
                    });
                    vm.taskCompletedForMe = response.data.data;
                    // variabili per la gestione della paginazione
                    vm.totalItems = response.data.total;
                    vm.queryCount = vm.totalItems;
                }, function (error) {
                    $log.error(error);
                });
        };

        //funzione richiamata quando si chiede una nuova "pagina" dei risultati
        vm.transition = function transition () {
            loadTaskCompleted()
        }
    }
})();
