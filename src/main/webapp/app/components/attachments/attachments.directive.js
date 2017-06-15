(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('attachments', attachmentsDirective);

    attachmentsDirective.$inject = ['dataService', '$sessionStorage', '$log', '$http', '$uibModal', 'utils'];

    function attachmentsDirective(dataService, $sessionStorage, $log, $http, $uibModal, utils) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/attachments/attachments.html',
            scope: {
              processInstanceId: '@?',
              taskId: '@?',
              showHistory: '@?'
            },
            link: function ($scope, element, attrs) {

                $scope.showHistory = $scope.showHistory === undefined ? true : $scope.showHistory;

                function setResponse (response) {
                    $scope.attachments = response.data;
                }
                function logError(response) {
                    $log.error(response);
                }


                if ($scope.processInstanceId !== undefined) {
                    $log.info("id in attachments "+ $scope.processInstanceId);
                    dataService.processInstances.getAttachments($scope.processInstanceId)
                    .then(setResponse, logError);

                } else {
                    if ($scope.taskId !== undefined) {
                        $log.info("id in attachments "+ $scope.taskId);
                        dataService.tasks.getAttachments($scope.taskId)
                        .then(setResponse, logError);

                    } else {
                        $log.error("processInstanceId e taskId non possono essere entrambi nulli!");
                    }
                }


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