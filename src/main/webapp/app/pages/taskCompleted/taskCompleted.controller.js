(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('TaskCompletedController', TaskCompletedController);

    TaskCompletedController.$inject = ['$scope', '$state', 'dataService', 'utils', '$log'];

    function TaskCompletedController($scope, $state, dataService, utils, $log) {
        var vm = this,
        loadTaskCompleted = function () {
            dataService.tasks.getTaskCompletedByMe(0, 1000)
                .then(function (response) {
                    response.data.data.forEach( function (task){
                        utils.refactoringVariables(task);
                    });
                    vm.taskCompletedForMe = response.data.data;
                }, function (error) {
                    $log.error(error);
                });
        };

        loadTaskCompleted();
    }
})();
