(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('processList', processListDirective);

  processListDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function processListDirective() {

    return {
          restrict: 'E',
          scope: {
            processes: '=',
            paging: '='
          },
          templateUrl: 'app/components/process-list/process-list.html',
          link: function ($scope) {
            $scope.selectVariables = function(process) {
                if(process.completed)
                    return process.variables;
                else
                    return process.queryVariables;
            }
          }
        };
    }
})();