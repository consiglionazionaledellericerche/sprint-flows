(function() {
  'use strict';

  angular.module('sprintApp')
  .controller('AddAttachmentModalController', AddAttachmentModalController);


  AddAttachmentModalController.$inject = ['$scope', '$uibModalInstance', 'dataService', 'processInstanceId', 'Upload', 'AlertService', '$log', 'utils'];

  function AddAttachmentModalController ($scope, $uibModalInstance, dataService, processInstanceId, Upload, AlertService, $log, utils) {

    var vm = this;
    vm.data = {processInstanceId: processInstanceId};
    $scope.attachments = {};
    $scope.data = {};
    $scope.data.processInstanceId = processInstanceId;

    $scope.submitDocumento = function(file) {

      utils.prepareForSubmit($scope.data, $scope.attachments);

      Upload.upload({
        url: 'api/attachments/'+ processInstanceId +'/data/new',
        data: $scope.data,
      }).then(function (response) {
        $scope.loadAttachments();
        $uibModalInstance.close();
        AlertService.success("Richiesta completata con successo");

      }, function (err) {
        $log.error(err);
        AlertService.error("Richiesta non riuscita<br>"+ err.data.message);
      });
    }
  }
})();