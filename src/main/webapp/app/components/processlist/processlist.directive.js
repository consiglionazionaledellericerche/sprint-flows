(function() {
  "use strict";

  angular.module("sprintApp").directive("processList", processList);

  processList.$inject = ["$log"];

  function processList($log) {
    return {
      restrict: "E",
      scope: {
        processes: "=",
        paging: "="
      },
      templateUrl: "app/components/processlist/processlist.html",
      link: function($scope) {
        $scope.processes.forEach(function(process) {
          var appo = process.variables;
          process.variables = {};
          appo.map(function(el) {
            process.variables[el.name] = el.value;
          });
        });
      }
    };
  }
})();
