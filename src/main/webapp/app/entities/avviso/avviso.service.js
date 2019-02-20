(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Avviso', Avviso);

    Avviso.$inject = ['$resource'];

    function Avviso ($resource) {
        var resourceUrl =  'api/avvisos/:id';

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
