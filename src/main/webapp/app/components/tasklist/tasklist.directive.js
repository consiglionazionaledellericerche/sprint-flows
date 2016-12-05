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
        refreshProgessBar: '=',
        selectProcessDefinitionKey: '=',
        processDefinitions: '='
      },
      templateUrl: 'app/components/tasklist/tasklist.html',
      link: function (scope, element, attrs) {

        scope.completed = attrs.completed;

        scope.pooled = [];

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
//        scope.claimTask = function (id, take) {
//          var user;
//          if (take === true) {
//            dataService.processinstances.claimTask(id).success(function (data) {
//              $log.debug(data);
//              scope.pooled[id] = user !== undefined;
//              scope.refreshProgessBar = !scope.refreshProgessBar;
//            });
//          } else {
//            dataService.processinstances.unclaimTask(id).success(function (data) {
//              $log.debug(data);
//              scope.pooled[id] = user !== undefined;
//              scope.refreshProgessBar = !scope.refreshProgessBar;
//            });
//          }
//        };
//
//        scope.modalTaskDiagram = function (task) {
//          var url = dataService.urls.proxy + 'service/api/workflow-instances/' + task.processId + '/diagram';
//          modalService.simpleModal(task.description, url);
//        };

//        scope.filterProcess = function (processDefinitionId) {
//          var selectProcessDefinitionKey;
//          _.each(scope.processDefinitions, function (value, key) {
//            if (value.entry.key === processDefinitionId.substr(0, processDefinitionId.indexOf(':'))) {
//              selectProcessDefinitionKey = value;
//              return false;
//            }
//          });
//          scope.selectProcessDefinitionKey = selectProcessDefinitionKey;
//        };
      }
    };


  }
})();