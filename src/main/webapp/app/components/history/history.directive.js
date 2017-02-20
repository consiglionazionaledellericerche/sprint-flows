(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('history', historyDirective);

  historyDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function historyDirective(dataService, $sessionStorage, $log) {

    return {
      restrict: 'E',
      scope: {
        tasks: '=',
        paging: '=',
        advanced: '=',
        detailed: '=',
        selectProcessDefinitionKey: '=',
        processDefinitions: '='
      },
      templateUrl: 'app/components/history/history.html',
      link: function (scope, element, attrs) {

        scope.$watch(attrs, function(oldValue, newValue) {
            $log.debug('value changed from '+ oldValue +' to '+ newValue);
        });
      }
    };
  }
})();