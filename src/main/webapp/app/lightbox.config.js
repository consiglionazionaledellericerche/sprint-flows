(function() {
    'use strict';

    angular
    .module('sprintApp')
    .config(lightboxConfig);

    lightboxConfig.$inject = ['LightboxProvider']

    function lightboxConfig(LightboxProvider) {
        LightboxProvider.calculateImageDimensionLimits = function (dimensions) {
            return {
                'maxWidth': dimensions.windowWidth >= 768 ? // default
                        dimensions.windowWidth - 92 :
                        dimensions.windowWidth - 52,
                 'maxHeight': 1600                           // custom
            };
        };
    }

})();
