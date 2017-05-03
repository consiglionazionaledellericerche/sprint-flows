(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('TaskCompletedController', TaskCompletedController);

    TaskCompletedController.$inject = ['$scope', '$state', 'dataService', '$log'];

    function TaskCompletedController($scope, $state, dataService, $log) {
        var vm = this,
        loadTaskCompleted = function () {
            dataService.tasks.getTaskCompleted(0, 1000)
                .then(function (response) {
                    vm.taskCompletedForMe = response.data.tasks;
                }, function (error) {
                    $log.error(error);
                });
        };

        loadTaskCompleted();
    }
})();
