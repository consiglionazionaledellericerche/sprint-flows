(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('ExternalMessage', ExternalMessage);

    ExternalMessage.$inject = ['$resource'];

    function ExternalMessage ($resource) {
        var resourceUrl =  'api/external-messages/:id';

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
