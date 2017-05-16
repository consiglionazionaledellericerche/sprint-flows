(function () {
    'use strict';

    angular
            .module('sprintApp')
            .controller('MyProcessInstancesController', MyProcessInstancesController);

    MyProcessInstancesController.$inject = ['$scope', '$rootScope', 'dataService', '$log'];

    function MyProcessInstancesController($scope, $rootScope, dataService, $log) {
        var vm = this;
        vm.myTasks = {total: 0};
        vm.pooledTasks = {total: 0};

        $scope.$watchGroup(['vm.order', 'current'], function () {
                $scope.loadMyProcessInstances();
        });

        $scope.loadMyProcessInstances = function () {
            dataService.processInstances.myProcessInstances(true, $rootScope.current, vm.order)
                    .then(function (response) {
                        vm.myProcessInstancesActive = response.data;
                    }, function (error) {
                        $log.error(error);
                    });

            dataService.processInstances.myProcessInstances(false, $rootScope.current, vm.order)
                    .then(function (response) {
                        vm.myProcessInstancesTerminated = response.data;
                    }, function (response) {
                        $log.error(response);
                    });
        }

        $scope.setActiveContent = function (choice) {
            $scope.activeContent = choice;
        }
    }
})();
