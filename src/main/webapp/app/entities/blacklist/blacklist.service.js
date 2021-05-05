(function() {
    'use strict';
    angular
        .module('flowsApp')
        .factory('Blacklist', Blacklist);

    Blacklist.$inject = ['$resource'];

    function Blacklist ($resource) {
        var resourceUrl =  'api/blacklists/:id';

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
