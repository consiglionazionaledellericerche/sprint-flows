(function() {
    'use strict';
    angular
        .module('sprintApp')
        .factory('ExternalMessage', ExternalMessage);

    ExternalMessage.$inject = ['$resource', 'DateUtils'];

    function ExternalMessage ($resource, DateUtils) {
        var resourceUrl =  'api/external-messages/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.creationDate = DateUtils.convertDateTimeFromServer(data.creationDate);
                        data.lastSendDate = DateUtils.convertDateTimeFromServer(data.lastSendDate);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
