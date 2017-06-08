(function() {
    'use strict';

    angular.module('sprintApp')
    .run(initialize);


    initialize.$inject = ['$rootScope', 'Lightbox', '$location'];

    function initialize ($rootScope, Lightbox, $location) {
        $rootScope.openDiagramModal = function(url) {
            Lightbox.openModal([url], 0);
        };

        var development = $location.$$host === 'localhost';
        $rootScope.development = development;
    }
})();