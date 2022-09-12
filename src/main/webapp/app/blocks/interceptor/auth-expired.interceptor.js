(function() {
    'use strict';

    angular
        .module('sprintApp')
        .factory('authExpiredInterceptor', authExpiredInterceptor);

    
    authExpiredInterceptor.$inject = ['$rootScope', '$q', '$injector', '$localStorage', '$sessionStorage', '$cookies', '$location'];

    function authExpiredInterceptor($rootScope, $q, $injector, $localStorage, $sessionStorage, $cookies, $location) {
        var service = {
            responseError: responseError
        };

        return service;

        function responseError(response) {
            if (response.status === 401) {
                delete $localStorage.authenticationToken;
                delete $sessionStorage.authenticationToken;
//                var Principal = $injector.get('Principal');
//                if (Principal.isAuthenticated()) {
//                    var Auth = $injector.get('Auth');
//                    Auth.authorize(true);
//                }
                 $cookies['KC_REDIRECT'] = '/#' + $location.url();
                 location.href = '/sso/login';            
            }
            return $q.reject(response);
        }
    }
})();
