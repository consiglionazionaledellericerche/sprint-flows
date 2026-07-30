(function() {
    /*jshint camelcase: false */
    'use strict';

    angular
        .module('sprintApp')
        .factory('AuthServerProvider', AuthServerProvider);

    AuthServerProvider.$inject = ['$http', '$localStorage', 'Base64', 'Principal', '$translate', '$q'];

    function AuthServerProvider ($http, $localStorage, Base64, Principal, $translate, $q) {
        return {
            login: function(credentials) {
                var data = "username=" + credentials.username + "&password="
                    + credentials.password + "&grant_type=password&scope=read%20write&" +
                    "client_secret=mySecretOAuthSecret&client_id=missioniApp";
                return $http.post('oauth/token', data, {
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                        "Accept": "application/json",
                        "Authorization": "Basic " + Base64.encode("sprintapp" + ':' + "mySecretOAuthSecret")
                    }
                }).success(function (response) {
                    var expiredAt = new Date();
                    expiredAt.setSeconds(expiredAt.getSeconds() + response.expires_in);
                    response.expires_at = expiredAt.getTime();
                    Principal.authenticate(response);
                    return response;
                });
            },
            logout: function(){ 
                location.href = "/sso/logout";
            },
            hasValidToken: function () {
                var token = this.getToken();
                return token && token.expires_at && token.expires_at > new Date().getTime();
            },
            profileInfo: function() {
                return $http.get('api/profile/info').success(function(response) {
                    return response;
                });
            }
        };

    }
})();
