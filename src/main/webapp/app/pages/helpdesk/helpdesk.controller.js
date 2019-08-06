(function () {
  'use strict';

  angular
    .module("sprintApp")
    .controller("HelpdeskController", HelpdeskController);

  HelpdeskController.$inject = ["$scope", "dataService", "$log", "$state", "AlertService", "utils"];

  function HelpdeskController($scope, dataService, $log, $state, AlertService, utils) {
    var vm = this, data;
    vm.hdDataModel = {};
    $scope.data = {};

    $scope.submitHelpdesk = function () {
      if ($scope.helpdeskForm.$invalid) {
        angular.forEach($scope.helpdeskForm.$error, function (field) {
          angular.forEach(field, function (errorField) {
            errorField.$setTouched();
          });
        });
        AlertService.warning("Inserire tutti i valori obbligatori.");
      } else {
        vm.hdDataModel.titolo = $scope.helpdeskModel.titolo;
        vm.hdDataModel.descrizione = $scope.helpdeskModel.descrizione;
        vm.hdDataModel.categoria = $scope.id;
        vm.hdDataModel.categoriaDescrizione = $scope.$parent.vm.hdDataModel;
        var request;
        if ($scope.attachments.allegato.data) {
          utils.prepareForSubmit($scope.data, $scope.attachments);
          data = $.extend($scope.data, vm.hdDataModel);
          request = dataService.helpdesk.sendWithAttachment(data);
        } else {
          request = dataService.helpdesk.sendWithoutAttachment(vm.hdDataModel);
        }
        request.then(function (response) {
          if (response.data.segnalazioneId) {
            AlertService.success("Segnalazione Helpdesk inviata con successo");
            $state.go('home');
          }
        },
          function (error) {
            $log.error(error);
            AlertService.error("Segnalazione NON inviata per problemi tecnici: riprovare in seguito");
          }
        );
      }
      $scope.fatto = function () {
        $state.go("home");
      };
    };
  }
})();
