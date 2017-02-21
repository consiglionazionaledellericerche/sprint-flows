(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('tasklist', taskListDirective);

  taskListDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function taskListDirective(dataService, $sessionStorage, $log) {

    return {
      restrict: 'E',
      scope: {
        tasks: '=',
        paging: '=',
        advanced: '=',
        detailed: '=',
        selectProcessDefinitionKey: '=',
        processDefinitions: '=',
        activeFlows: '='
      },
      templateUrl: 'app/components/tasklist/tasklist.html',
      link: function (scope, element, attrs) {

        scope.activeFlows = attrs.activeFlows

        scope.completed = attrs.completed;

        scope.pooled = [];

        // trucco per dare il nome alle variabili nella mappa
        // invece di un array di mappe
        scope.$watch('tasks', function() {
          if (scope.tasks !== undefined) {
            scope.tasks.forEach(function(task) {
              task.variabili = {};
              task.variables.forEach(function (variable) {
                task.variabili[variable.name] = variable.value;
              });
            });
          }
        })

        scope.claimTask = function (id, take) {
          var user;
          var promise;
          dataService.tasks.claim(id, take).success(function (data) {
            $log.debug(data);
            scope.pooled[id] = user !== undefined;
            scope.$parent.loadTasks();
          });
        };
      }
    };


  }
})();