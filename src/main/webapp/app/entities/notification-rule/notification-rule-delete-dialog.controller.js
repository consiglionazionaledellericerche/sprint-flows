(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('NotificationRuleDeleteController',NotificationRuleDeleteController);

    NotificationRuleDeleteController.$inject = ['$uibModalInstance', 'entity', 'NotificationRule'];

    function NotificationRuleDeleteController($uibModalInstance, entity, NotificationRule) {
        var vm = this;

        vm.notificationRule = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;
        
        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            NotificationRule.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
