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


          $scope.experience = function(experience, columns) {
              $uibModal.open({
                  templateUrl: 'app/components/metadatum/detailsMetadatum.modal.html',
                  controller: 'DetailsMetadatumModalController',
                  controllerAs: 'vm',
                  size: 'md',
                  resolve: {
                      experience: function() {
                          return experience;
                      },
                      columns: function() {
                          return columns;
                      }
                  }
              })
          };
        }
      }
    }
  }
})();