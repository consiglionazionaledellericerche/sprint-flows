(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('NotificationRuleDialogController', NotificationRuleDialogController);

    NotificationRuleDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'NotificationRule'];

    function NotificationRuleDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, NotificationRule) {
        var vm = this;

        vm.notificationRule = entity;
        vm.clear = clear;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.notificationRule.id !== null) {
                NotificationRule.update(vm.notificationRule, onSaveSuccess, onSaveError);
            } else {
                NotificationRule.save(vm.notificationRule, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:notificationRuleUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
