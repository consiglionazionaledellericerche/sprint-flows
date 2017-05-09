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
        processDefinitions: '='
      },
      templateUrl: 'app/components/tasklist/tasklist.html',
      link: function (scope, element, attrs) {

        scope.actionButtons = attrs.actionButtons

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
                var processDefinitionId = task.processDefinitionId.split(":")[0];
                task.searchView = 'api/views/'+ processDefinitionId +'/search'
              });
            });
          }
        })

        scope.claimTask = function (taskId, take) {
          var user;
          dataService.tasks.claim(taskId, take).success(function (data) {
            $log.debug(data);
            scope.pooled[taskId] = user !== undefined;
            scope.$parent.loadTasks();
          });
        };
      }
    };
  }
})();