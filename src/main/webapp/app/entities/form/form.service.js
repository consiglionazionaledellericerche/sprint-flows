(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('Form', Form);

    Form.$inject = ['$resource'];

    function Form ($resource) {
        var resourceUrl =  'api/forms/:id';

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
