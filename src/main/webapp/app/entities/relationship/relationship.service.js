(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Relationship', Relationship);

    Relationship.$inject = ['$resource'];

    function Relationship ($resource) {
        var resourceUrl =  'api/relationships/:id';

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
