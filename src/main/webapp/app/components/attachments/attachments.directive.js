(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('attachments', attachmentsDirective);

    attachmentsDirective.$inject = ['dataService', '$sessionStorage', '$log', '$http', '$uibModal', 'utils', 'Principal'];

    function attachmentsDirective(dataService, $sessionStorage, $log, $http, $uibModal, utils, Principal) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/attachments/attachments.html',
            scope: {
                processInstanceId: '@?',
                attachments: '@?',
                taskId: '@?',
                showHistory: '@?'
            },
            link: function($scope, element, attrs) {
                $scope.showHistory = $scope.showHistory === undefined ? true : $scope.showHistory;

                function setResponse(response) {
                    $scope.attachments = response.data;
                }

                function logError(response) {
                    $log.error(response);
                }


                $scope.loadAttachments = function() {
                    if ($scope.processInstanceId !== undefined) {
                        $log.info("id in attachments " + $scope.processInstanceId);
                        dataService.processInstances.getAttachments($scope.processInstanceId)
                            .then(setResponse, logError);
                    } else {
                        if ($scope.taskId !== undefined) {
                            $log.info("id in attachments " + $scope.taskId);
                            dataService.tasks.getAttachments($scope.taskId)
                                .then(setResponse, logError);
                        } else {
                            $log.error("processInstanceId e taskId non possono essere entrambi nulli!");
                        }
                    }

                }


                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                }


                $scope.$watchGroup(['attachments'], function() {
                    if (typeof $scope.attachments === 'string' && $scope.attachments.length > 0)
                        $scope.attachments = JSON.parse($scope.attachments);
                });


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

                Principal.hasAuthority("ROLE_ADMIN").then(function(result) {
                    $scope.isPubblicatore = result;
                });

                $scope.showFileActions = function(attachment) {
                    $uibModal.open({
                        templateUrl: 'app/components/attachments/attachmentactions.modal.html',
                        controller: 'AttachmentActionsModalController',
                        controllerAs: 'vm',
                        scope: $scope,
                        resolve: {
                            attachment: function() {
                                return attachment;
                            },
                            processInstanceId: function() {
                                return $scope.processInstanceId;
                            }
                        }
                    });
                };

            }
        }
    }
})();