(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Cnrauthority', Cnrauthority);

    Cnrauthority.$inject = ['$resource'];

    function Cnrauthority ($resource) {
        var resourceUrl =  'api/cnrauthorities/:id';

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
