(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FaqDialogController', FaqDialogController);

    FaqDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Faq'];

    function FaqDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Faq) {
        var vm = this;

        vm.faq = entity;
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
            if (vm.faq.id !== null) {
                Faq.update(vm.faq, onSaveSuccess, onSaveError);
            } else {
                Faq.save(vm.faq, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:faqUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
