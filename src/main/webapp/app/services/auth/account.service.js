(function() {
    'use strict';

    angular
        .module('sprintApp')
        .factory('Account', Account);

    Account.$inject = ['$resource'];

    function Account ($resource) {
        var service = $resource('api/ldap-account', {}, {
            'get': { method: 'GET', params: {}, isArray: false,
                interceptor: {
                    response: function(response) {
                        // expose response
                        response.data.langKey = 'it';
                        return response;
                    }
                }
            }
        });

        return service;
    }
})();
