(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('taskList', taskListDirective);

  taskListDirective.$inject = ['dataService', '$log'];

  function taskListDirective(dataService, $log) {

    return {
      restrict: 'E',
      scope: {
        tasks: '='
      },
      templateUrl: 'app/components/task-list/task-list.html',
      link: function (scope, element, attrs) {

        scope.actionButtons = attrs.actionButtons

        scope.completed = attrs.completed;

        scope.pooled = [];

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