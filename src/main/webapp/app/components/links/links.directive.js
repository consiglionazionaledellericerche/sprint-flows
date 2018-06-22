(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('links', linksDirective);

//    linksDirective.$inject = [];

    function linksDirective() {
        return {
            restrict: 'E',
            scope: {
                links: '='
            },
            templateUrl: 'app/components/links/links.html'
        };
    }
})();