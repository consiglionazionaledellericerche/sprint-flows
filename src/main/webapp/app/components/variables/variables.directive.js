(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('variables', variablesDirective);

  variablesDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function variablesDirective(dataService, $sessionStorage, $log) {

    return {
          restrict: 'E',
          scope: {
            variables: '=',
            labelClass: '='
          },
          templateUrl: 'app/components/variables/variables.html'
        };
    }
})();