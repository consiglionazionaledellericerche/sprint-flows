(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Dynamiclist', Dynamiclist);

    Dynamiclist.$inject = ['$resource'];

    function Dynamiclist ($resource) {
        var resourceUrl =  'api/dynamiclists/:id';

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
