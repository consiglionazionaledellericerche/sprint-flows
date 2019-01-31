(function() {
    'use strict';

    angular
        .module('sprintApp')
        .constant('paginationConstants', {
            'itemsPerPage': 20 //todo: refactoring itemsPerPage => itemsForPage
        });
})();
