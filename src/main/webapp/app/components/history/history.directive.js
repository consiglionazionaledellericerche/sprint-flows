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
        startTask: '='
      },
      templateUrl: 'app/components/history/history.html'
    };
  }
})();