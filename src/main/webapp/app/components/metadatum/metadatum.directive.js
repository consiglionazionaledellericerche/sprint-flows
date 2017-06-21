(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('metadatum', metadatum);

  metadatum.$inject = ['dataService', '$log'];

  function metadatum(dataService, $log) {

    return {
      restrict: 'E',
      templateUrl: 'app/components/metadatum/metadatum.html',
      scope: {
        label: '@',
        value: '@',
        type: '@?',
        columns: '=?',
        rows: '=?'
      },
      link: function ($scope, element, attrs) {

        $scope.type = $scope.type || "text";
        $scope.attrs = attrs;

        if ($scope.type === 'table') {
          if ($scope.columns === undefined)
            throw "Per metadati 'table' e' necessario fornire l'attributo 'columns'";


          $scope.$watch('rows', function(newValue) {
            if (newValue != undefined)
              $scope.righe = JSON.parse(newValue);
          });

        }
      }
    }
  }
})();