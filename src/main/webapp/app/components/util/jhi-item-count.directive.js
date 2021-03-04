(function() {
    'use strict';

    var jhiItemCount = {
        template: '<div class="info">' +
                    'Risultati da {{(($ctrl.page - 1) * $ctrl.itemsPerPage) == 0 ? 1 : (($ctrl.page - 1) * $ctrl.itemsPerPage + 1)}} a ' +
                    '{{($ctrl.page * $ctrl.itemsPerPage) < $ctrl.queryCount ? ($ctrl.page * $ctrl.itemsPerPage) : $ctrl.queryCount}} ' +
                    'di {{$ctrl.queryCount}} elementi.' +
                '</div>',
        bindings: {
            page: '<',
            queryCount: '<total',
            itemsPerPage: '<'
        }
    };

    angular
        .module('sprintApp')
        .component('jhiItemCount', jhiItemCount);
})();
