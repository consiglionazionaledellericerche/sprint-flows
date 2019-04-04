(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CartController', CartController);

    CartController.$inject = ['$scope', '$state', '$localStorage', 'dataService', '$uibModal'];

    function CartController($scope, $state, $localStorage, dataService, $uibModal) {
        var vm = this;
        $scope.$localStorage = $localStorage;

        $scope.removeAll = function() {
            delete $localStorage.cart;
            $state.go('availableTasks')
        }

        $scope.signAll = function() {
            dataService.signMany(Object.keys($localStorage.cart));
        }

        $scope.openSignModal = function() {
            $uibModal.open({
                templateUrl: 'app/pages/cart/signmany.modal.html',
                controller: 'SignManyModalController',
                controllerAs: 'vm',
                size: 'md'
            })
        };
    }

})();