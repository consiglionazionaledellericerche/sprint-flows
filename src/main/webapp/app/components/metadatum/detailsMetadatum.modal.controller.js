(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('DetailsMetadatumModalController', DetailsMetadatumModalController);

    DetailsMetadatumModalController.$inject = ['$uibModalInstance', 'detailsLabel', 'row', 'columns', 'modalColumns'];

    function DetailsMetadatumModalController ($uibModalInstance, detailsLabel, row, columns, modalColumns) {
        var vm = this;
        vm.row = row;
        vm.columns = modalColumns ? columns.concat(modalColumns) : columns;
        vm.detailsLabel = detailsLabel || 'Esperienze';
    }
})();