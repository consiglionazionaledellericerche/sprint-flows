(function() {
    /*jshint camelcase: false */
    'use strict';

    angular
        .module('sprintApp')
        .factory('AuthServerProvider', AuthServerProvider);

    AuthServerProvider.$inject = ['$http', '$localStorage', 'Base64', 'Principal', '$translate', '$q'];

    function AuthServerProvider ($http, $localStorage, Base64, Principal, $translate, $q) {
        var service = {
            getToken: getToken,
            login: login,
            logout: logout
        };

        return service;

        function getToken () {
            return $localStorage.authenticationToken;
        }

        function login (credentials) {
            var data = 'username=' +  encodeURIComponent(credentials.username) + '&password=' +
                encodeURIComponent(credentials.password) + '&grant_type=password&scope=read%20write&' +
                'client_secret=my-secret-token-to-change-in-production&client_id=sprintapp';

            return $http.post('oauth/token', data, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json',
                    'Authorization': 'Basic ' + Base64.encode('sprintapp' + ':' + 'my-secret-token-to-change-in-production')
                }
            }).success(authSucess);

            function authSucess (response) {
                var expiredAt = new Date();
                expiredAt.setSeconds(expiredAt.getSeconds() + response.expires_in);
                response.expires_at = expiredAt.getTime();
                $localStorage.authenticationToken = response;

                Principal.identity(true).then(function(account) {
                    // After the login the language will be changed to
                    // the language selected by the user during his registration
                    if (account!== null) {
                        $translate.use(account.langKey).then(function () {
                            $translate.refresh();
                        });
                    }
                    $q.defer().resolve(data);
                });
                return angular.noop;
            }
        }

        function logout () {
            $http.post('api/logout').then(function() {
                delete $localStorage.authenticationToken;
            });
        }
    }
})();
