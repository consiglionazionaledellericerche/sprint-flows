(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('DetailModalController', DetailModalController);

    DetailModalController.$inject = ['$uibModalInstance', 'variables', 'title'];

    function DetailModalController ($uibModalInstance, variables, title) {
        var vm = this;

        vm.cancel = cancel;
        vm.baseName = title;
        vm.variables = variables;

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }
    }
})();
