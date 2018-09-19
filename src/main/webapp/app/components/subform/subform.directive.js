(function() {
    'use strict';

    /**
     * Author: Martin
     *
     *  Uso:
     *  ----
     *
     *
     *  Inserire un entita' form nel db (mettiamo col nome X)
     *
     *  richiamare la form in un altra form con
     *
     *  <subform
     *    multiple=true
     *    subform-name="X"
     *    label="Questa e' una subform"
     *    ng-model="vm.data.nomeDelModel"
     *    json="'[{}]'" />
     *
     *  Il parametro json serve per inizializzare il valore della subform ed e' una stringa
     *
     *  Per inizializzare una subform vuota usare
     *  json="'[{}]'" (ricordarsi le virgolette interne - e' una stringa)
     *
     *  Per inizializzare una subform con parametri inseriti precedentemente usare (per esempio)
     *  json="vm.data.entity.variabili['nomeDelModel_json']"
     *
     *  Con questo meccanismo e' possibile inizializzare il model con qualunque valore arbitrario
     */

    angular.module('sprintApp')
    .directive('subform', subform);

    subform.$inject = ['dataService', '$log'];

    function subform(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/components/subform/subform.html',
            scope: {
                ngModel: '=',
                json: '@?',
                label: '@',
                multiple: '@',
                subformName: '@',
                min: '@?',
                max: '@?'
            },
            link: function ($scope, element, attrs) {
                var subform = this;
                $scope.subform = subform;
                $scope.min = $scope.min || 1;
                $scope.max = $scope.max || 999;

                if ($scope.json !== undefined)
                  $scope.ngModel = JSON.parse($scope.json);

                subform.formUrl = 'api/forms/'+ $scope.$parent.vm.processDefinitionKey +"/"+$scope.$parent.vm.processVersion +"/"+ $scope.subformName;

                $scope.addRow = function() {
                    if ($scope.ngModel.length < $scope.max)
                        $scope.ngModel.push({});
                    return false;
                };
                $scope.removeRow = function() {
                    if ($scope.ngModel.length > $scope.min)
                        $scope.ngModel.pop();
                    return false;
                };
            }
        }
    }
})();