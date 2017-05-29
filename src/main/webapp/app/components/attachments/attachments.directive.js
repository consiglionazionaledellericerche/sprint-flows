(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('attachments', attachmentsDirective);

    attachmentsDirective.$inject = ['dataService', '$sessionStorage', '$log', '$http', '$uibModal', 'utils'];

    function attachmentsDirective(dataService, $sessionStorage, $log, $http, $uibModal, utils) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/attachments/attachments.html',
            link: function ($scope, element, attrs) {

                $log.info("id in attachments "+ $scope.processInstanceId);

                dataService.processInstances.attachments($scope.processInstanceId)
                .then(function(response) {
                    $log.info(response);
                    $scope.attachments = response.data;
                }, function(response) {
                    $log.info(response);
                });

                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                }

                $scope.showFileHistory = function(attachmentName) {
                    $scope.fileHistory = [];
                    dataService.processInstances.attachmentHistory($scope.processInstanceId, attachmentName)
                    .then(function(response) {
                        $scope.fileHistory = response.data;
                        $uibModal.open({
                            templateUrl: 'fileHistoryModal.html',
                            scope: $scope
                        });
                    });

                };
            }
        }
    }
})();