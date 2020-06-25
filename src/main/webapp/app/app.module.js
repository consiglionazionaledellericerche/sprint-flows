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
            'bootstrapLightbox',
            'toggle-switch',
            'ui.select',
            'ngSanitize',
            'dc.inputAddOn',
            'angularTrix'
        ]).run(run);

    run.$inject = ['stateHandler', 'translationHandler'];

    function run(stateHandler, translationHandler) {
        stateHandler.initialize();
        translationHandler.initialize();
    }
    
    function isChromeOrFirefox() {
        var is_firefox, is_chrome, is_edge;
        is_firefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
        is_chrome = navigator.userAgent.toLowerCase().indexOf('chrome') > -1;
        is_edge = window.navigator.userAgent.indexOf('Edge') > -1;
        return ((is_firefox || is_chrome) && !is_edge);
    }

    if(!isChromeOrFirefox())
        $(".browsehappy").show();
})();
