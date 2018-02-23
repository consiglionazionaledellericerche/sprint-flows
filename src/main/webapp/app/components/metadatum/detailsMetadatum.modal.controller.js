(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('DetailsMetadatumModalController', DetailsMetadatumModalController);

    DetailsMetadatumModalController.$inject = ['$uibModalInstance', 'experience', 'columns'];

    function DetailsMetadatumModalController ($uibModalInstance, experience, columns) {
        var vm = this;
        vm.experience = experience;
        vm.columns = columns;
    }
})();