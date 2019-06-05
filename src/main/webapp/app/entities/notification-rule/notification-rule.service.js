(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('NotificationRule', NotificationRule);

    NotificationRule.$inject = ['$resource'];

    function NotificationRule ($resource) {
        var resourceUrl =  'api/notification-rules/:id';

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
