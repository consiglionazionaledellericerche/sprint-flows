(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FaqDetailController', FaqDetailController);

    FaqDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Faq'];

    function FaqDetailController($scope, $rootScope, $stateParams, previousState, entity, Faq) {
        var vm = this;

        vm.faq = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:faqUpdate', function(event, result) {
            vm.faq = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
