(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('attachments', attachmentsDirective);

    attachmentsDirective.$inject = ['dataService', '$sessionStorage', '$log'];

    function attachmentsDirective(dataService, $sessionStorage, $log) {

        return {
            restrict: 'E',
            scope: {
                processInstanceId: '='
            },
            templateUrl: 'app/components/attachments/attachments.html',
            link: function (scope, element, attrs) {

                dataService.processInstances.attachments(attrs.processInstanceId)
                .then(function(response) {
                    $log.info(response);
                    scope.attachments = response.data;
                }, function(response) {
                    $log.info(response);
                });
            }
        }
    }
})();