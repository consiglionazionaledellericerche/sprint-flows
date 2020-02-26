(function() {
  'use strict';

  angular.module('sprintApp')
  .controller('AttachmentActionsModalController', AttachmentActionsModalController);


  AttachmentActionsModalController.$inject = ['$scope', '$uibModalInstance', 'dataService', 'attachment', 'businesskey', 'processInstanceId', 'Upload', 'AlertService', '$log', 'utils'];

  function AttachmentActionsModalController ($scope, $uibModalInstance, dataService, attachment, businesskey, processInstanceId, Upload, AlertService, $log, utils) {

    var vm = this;

    $scope.attachments = {};
    $scope.attachments[attachment.name] = attachment;
    $scope.data = {};
    $scope.data.processInstanceId = processInstanceId;

    vm.attachment = attachment;

    vm.pubblicatoTrasparenza = vm.attachment.stati.indexOf("PubblicatoTrasparenza") >= 0;
    vm.businesskey = businesskey;
    $scope.pubblicaDocumentoTrasparenza = function(flag) {
      dataService.attachments.pubblicaDocumentoTrasparenza(processInstanceId, attachment.name, flag)
      .then(function() {
        $scope.loadAttachments();
        $uibModalInstance.close();
        AlertService.success("Richiesta completata con successo");
      }, function() {
        console.log("error")
      })
    }
    
    vm.pubblicatoUrp = vm.attachment.stati.indexOf("PubblicatoUrp") >= 0;
    $scope.data = {};
    
    $scope.pubblicaDocumentoUrp = function(flag) {
        dataService.attachments.pubblicaDocumentoUrp(processInstanceId, attachment.name, flag)
        .then(function() {
          $scope.loadAttachments();
          $uibModalInstance.close();
          AlertService.success("Richiesta completata con successo");
        }, function() {
          console.log("error")
        })
      }

    $scope.submitAggiornaDocumento = function(file) {

      utils.prepareForSubmit($scope.data, $scope.attachments);
      var suffix = $scope.data.tipoModifica || "";

      Upload.upload({
        url: 'api/attachments/'+ processInstanceId +'/'+ attachment.name +'/data/'+ suffix,
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