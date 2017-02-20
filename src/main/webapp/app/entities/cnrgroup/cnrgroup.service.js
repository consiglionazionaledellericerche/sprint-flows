(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Cnrgroup', Cnrgroup);

    Cnrgroup.$inject = ['$resource'];

    function Cnrgroup ($resource) {
        var resourceUrl =  'api/cnrgroups/:id';

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
