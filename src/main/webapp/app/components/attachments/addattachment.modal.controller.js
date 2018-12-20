(function() {
  'use strict';

  angular.module('sprintApp')
  .controller('AddAttachmentModalController', AddAttachmentModalController);


  AddAttachmentModalController.$inject = ['$scope', '$uibModalInstance', 'dataService', 'processInstanceId', 'Upload', 'AlertService', '$log'];

  function AddAttachmentModalController ($scope, $uibModalInstance, dataService, processInstanceId, Upload, AlertService, $log) {

    var vm = this;
    vm.data = {processInstanceId: processInstanceId};
    $scope.attachments = {};

    $scope.submitDocumento = function(file) {

      Upload.upload({
        url: 'api/attachments/'+ processInstanceId +'/'+ 'nuovoDocumento' + '/data',
        data: $scope.attachments,
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