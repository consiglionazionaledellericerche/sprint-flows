(function() {
  "use strict";

  angular.module("sprintApp").directive("archiveProcessList", processList);

  processList.$inject = ["$log"];

  function processList($log) {
    return {
      restrict: "E",
      scope: {
        processes: "=",
        paging: "="
      },
      templateUrl: "app/components/archiveprocesslist/archiveprocesslist.html",
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
