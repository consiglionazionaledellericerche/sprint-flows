(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('MembershipDetailController', MembershipDetailController);

    MembershipDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Membership'];

    function MembershipDetailController($scope, $rootScope, $stateParams, previousState, entity, Membership) {
        var vm = this;

        vm.membership = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:membershipUpdate', function(event, result) {
            vm.membership = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
