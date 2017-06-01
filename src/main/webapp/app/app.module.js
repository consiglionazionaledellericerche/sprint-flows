(function() {
    'use strict';

    angular
        .module('sprintApp', [
            'ngStorage',
            'tmh.dynamicLocale',
            'pascalprecht.translate',
            'ngResource',
            'ngCookies',
            'ngAria',
            'ngCacheBuster',
            'ngFileUpload',
            'ui.bootstrap',
            'ui.bootstrap.datetimepicker',
            'ui.router',
            'infinite-scroll',
            // jhipster-needle-angularjs-add-module JHipster will add new module here
            'angular-loading-bar',
            'ui.ace',
            'ngJsTree',
            'bootstrapLightbox'
        ])
        .run(run);

    run.$inject = ['stateHandler', 'translationHandler', '$rootScope', 'Lightbox'];

    function run(stateHandler, translationHandler, $rootScope, Lightbox) {
        stateHandler.initialize();
        translationHandler.initialize();

        $rootScope.openDiagramModal = function(url) {
            Lightbox.openModal([url], 0);
        }
    }
})();
