(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('metadatum', metadatum);

  metadatum.$inject = ['dataService', '$uibModal', '$log'];

  function metadatum(dataService, $uibModal, $log) {

    return {
      restrict: 'E',
      templateUrl: 'app/components/metadatum/metadatum.html',
      scope: {
        label: '@',
        value: '@',
        type: '@?',
        columns: '=?',
        modalColumns: '=?',
        detailsLabel: '@?',
        rows: '=?',
        details: '@'
      },
      link: function ($scope, element, attrs) {

        $scope.type = $scope.type || "text";
        $scope.attrs = attrs;


        if ($scope.type === 'table') {
            $scope.details = attrs.details;
            if ($scope.columns === undefined)
                throw "Per metadati 'table' e' necessario fornire l'attributo 'columns'";


          $scope.$watch('rows', function(newValue) {
            if (newValue != undefined)
              $scope.righe = JSON.parse(newValue);
          });


          $scope.detailsModal = function(row) {
              $uibModal.open({
                  templateUrl: 'app/components/metadatum/detailsMetadatum.modal.html',
                  controller: 'DetailsMetadatumModalController',
                  controllerAs: 'vm',
                  size: 'md',
                  resolve: {
                      row: function() {
                          return row;
                      },
                      columns: function() {
                          return $scope.columns;
                      },
                      modalColumns: function() {
                          return $scope.modalColumns;
                      },
                      detailsLabel: function() {
                          return $scope.detailsLabel;
                      },
                  }
              })
          };
        }
      }
    }
  }
})();