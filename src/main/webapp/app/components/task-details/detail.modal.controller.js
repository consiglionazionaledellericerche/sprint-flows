(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('DetailModalController', DetailModalController);

    DetailModalController.$inject = ['$uibModalInstance', 'task'];

    function DetailModalController ($uibModalInstance, task) {
        var vm = this;

        vm.cancel = cancel;
        vm.baseName = task.name;
        vm.task = task;

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }
    }
})();
