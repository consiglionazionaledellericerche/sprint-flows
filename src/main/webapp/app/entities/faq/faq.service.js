(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Faq', Faq);

    Faq.$inject = ['$resource'];

    function Faq ($resource) {
        var resourceUrl =  'api/faqs/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
