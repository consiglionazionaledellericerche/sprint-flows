(function() {
  "use strict";

  angular
    .module("sprintApp")
    .controller("HelpdeskController", HelpdeskController);

  HelpdeskController.$inject = ["$scope", "dataService", "$log", "$uibModal", "$state", "AlertService"];

  function HelpdeskController($scope, dataService, $log, $uibModal, $state, AlertService) {
    var vm = this;
        vm.hdDataModel = {};

    $scope.submitHelpdesk = function() {
      if ($scope.helpdeskForm.$invalid) {
        angular.forEach($scope.helpdeskForm.$error, function(field) {
          angular.forEach(field, function(errorField) {
            errorField.$setTouched();
          });
        });
        AlertService.warning("Inserire tutti i valori obbligatori.");
      } else {
          vm.hdDataModel.titolo = $scope.helpdeskModel.titolo;
          vm.hdDataModel.descrizione = $scope.helpdeskModel.descrizione;
          vm.hdDataModel.categoria = $scope.id;
          vm.hdDataModel.categoriaDescrizione = $scope.$parent.vm.hdDataModel;

          dataService.helpdesk.sendWithoutAttachment(vm.hdDataModel).then(
            function(response) {
              if (response.data.segnalazioneId) {
                $uibModal.open({
                  template: `<div class="modal-header">
                                <h4 class="modal-title">Segnalazione inviata correttamente</h4>
                              </div>
                              <div class="modal-body">
                                <button class="btn btn-primary" type="button" ng-click="fatto()"><span class="glyphicon glyphicon-remove"></span> Chiudi</button>
                              </div>`,
                  scope: $scope
                });
              }
            },
            function(error) {
              $log.error(error);
              $uibModal.open({
                template: `<div class="modal-header">
                              <h4 class="modal-title">Segnalazione NON inviata per problemi tecnici: riprovare in seguito</h4>
                           </div>
                           <div class="modal-body">
                             <button class="btn btn-primary" type="button" ng-click="$dismiss()"><span class="glyphicon glyphicon-remove"></span> Chiudi</button>
                           </div>`
              });
            }
          );
      }
      $scope.fatto = function() {
        $state.go("home");
      };
    };
  }
})();
