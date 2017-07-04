(function() {
  'use strict';

  angular.module('sprintApp')
  .controller('AttachmentActionsModalController', AttachmentActionsModalController);


  AttachmentActionsModalController.$inject = ['$scope', '$uibModalInstance', 'dataService', 'attachment', 'processInstanceId', 'Upload', 'AlertService'];

  function AttachmentActionsModalController ($scope, $uibModalInstance, dataService, attachment, processInstanceId, Upload, AlertService) {

    var vm = this;

    vm.attachment = attachment;

    vm.pubblicato = vm.attachment.stati.indexOf("Pubblicato") >= 0;

    $scope.pubblicaDocumento = function(flag) {
      dataService.attachments.pubblicaDocumento(processInstanceId, attachment.name, flag)
      .then(function() {
        $scope.loadAttachments();
        $uibModalInstance.close();
        AlertService.success("Richiesta completata con successo");
      }, function() {
        console.log("error")
      })
    }

    $scope.submitAggiornaDocumento = function(file) {

      Upload.upload({
        url: 'api/attachments/'+ processInstanceId +'/'+ attachment.name +'/data',
        data: vm.data,
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