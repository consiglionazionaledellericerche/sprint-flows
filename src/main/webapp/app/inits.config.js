//(function() {
//    'use strict';
//
//    angular.module('sprintApp')
//    .run('inits', initialize);
//
//
//    initialize.$inject = ['$rootScope', 'Lightbox'];
//
//    function initialize ($rootScope, Lightbox) {
//        $rootScope.openDiagramModal = function(url) {
//            Lightbox.openModal([url], 0);
//        };
//
//        var development = $location.$$host === 'localhost';
//        $rootScope.development = development;
//    }
//})();