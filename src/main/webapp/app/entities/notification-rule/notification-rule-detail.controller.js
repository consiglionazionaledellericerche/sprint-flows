(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('NotificationRuleDetailController', NotificationRuleDetailController);

    NotificationRuleDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'NotificationRule'];

    function NotificationRuleDetailController($scope, $rootScope, $stateParams, previousState, entity, NotificationRule) {
        var vm = this;

        vm.notificationRule = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:notificationRuleUpdate', function(event, result) {
            vm.notificationRule = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
