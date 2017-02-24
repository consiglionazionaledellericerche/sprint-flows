(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('processList', processListDirective);

  processListDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function processListDirective(dataService, $sessionStorage, $log) {

    return {
          restrict: 'E',
          scope: {
            processes: '=',
            paging: '='
          },
          templateUrl: 'app/components/process-list/process-list.html'
        };
    }
})();