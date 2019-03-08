(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CartController', CartController);

    CartController.$inject = ['$scope', '$state'];

    function CartController($scope, $state) {
        var vm = this;

    }

})();