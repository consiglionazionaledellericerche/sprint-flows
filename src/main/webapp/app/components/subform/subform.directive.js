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
     *    ng-model="data.nomeDelModel"
     *    autofill />
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
                label: '@',
                multiple: '@',
                subformName: '@',
                min: '@?',
                max: '@?',
                autofill: '@?',
                buttonPosition: '@?'
            },
            link: function ($scope, element, attrs) {
                $scope.min = $scope.min || 1;
                $scope.max = $scope.max || 999;
                $scope.ngModel = $scope.ngModel || [{}]

                $scope.processDefinitionKey = $scope.processDefinitionKey || $scope.$parent.processDefinitionKey;
                $scope.processVersion = $scope.processVersion || $scope.$parent.processVersion;
                $scope.formUrl = 'api/forms/'+ $scope.processDefinitionKey +"/"+ $scope.processVersion +"/"+ $scope.subformName;
                if ( $scope.buttonPosition === undefined) {
                       $scope.buttonPosition='top';
                }
//                if ($scope.json !== undefined)
//                  $scope.ngModel = JSON.parse($scope.json);
                if ('autofill' in attrs) {
                    var jsonName = attrs.ngModel.split('.').pop() + "_json";
                    $scope.ngModel = JSON.parse($scope.$parent.data.entity.variabili[jsonName]);
                }

                $scope.addRow = function() {
                    if ($scope.ngModel.length < $scope.max)
                        $scope.ngModel.push({});
                    return false;
                };
                $scope.removeRow = function(index) {
                    if ($scope.buttonPosition==='inner' ){
                        $scope.ngModel.splice(index, 1);
                    }else{
                       if ($scope.ngModel.length > $scope.min)
                         $scope.ngModel.pop();
                    }
                    return false;
                };
            }
        }
    }
})();