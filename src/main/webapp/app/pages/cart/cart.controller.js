(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CartController', CartController);

    CartController.$inject = ['$scope', '$state'];

    function CartController($scope, $state) {
        var vm = this;

    }
    
    
    angular.module('aprintApp').factory('cartService', cartService);
    
    cartService.$inject = ['$rootScope']
    
    function cartService($rootScope) {
    	
    	return {
    		addToCart: function(taskId) {
    			$rootScope.cart = $rootScope.cart || [];
    			$rootScope.cart[taskId] = taskId;
    		},
    		clearCart: function() {
    			$rootScope.cart = {};
    		}
    	}
    }

})();