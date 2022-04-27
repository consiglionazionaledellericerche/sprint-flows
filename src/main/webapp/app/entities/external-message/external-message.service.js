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
                        data.creationDate = DateUtils.convertLocalDateFromServer(data.creationDate);
                        data.lastSendDate = DateUtils.convertLocalDateFromServer(data.lastSendDate);
                    }
                    return data;
                }
            },
            'update': {
                method: 'PUT',
                transformRequest: function (data) {
                    data.creationDate = DateUtils.convertLocalDateToServer(data.creationDate);
                    data.lastSendDate = DateUtils.convertLocalDateToServer(data.lastSendDate);
                    return angular.toJson(data);
                }
            },
            'save': {
                method: 'POST',
                transformRequest: function (data) {
                    data.creationDate = DateUtils.convertLocalDateToServer(data.creationDate);
                    data.lastSendDate = DateUtils.convertLocalDateToServer(data.lastSendDate);
                    return angular.toJson(data);
                }
            }
        });
    }
})();
