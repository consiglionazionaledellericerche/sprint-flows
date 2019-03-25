(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CartController', CartController);

    CartController.$inject = ['$scope', '$state', '$localStorage', 'dataService'];

    function CartController($scope, $state, $localStorage, dataService) {
        var vm = this;
        $scope.$localStorage = $localStorage;

        $scope.removeAll = function() {
            delete $localStorage.cart;
            $state.go('availableTasks')
        }

        $scope.signAll = function() {
            dataService.signMany(Object.keys($localStorage.cart));
        }
    }

})();