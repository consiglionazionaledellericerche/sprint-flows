(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('taskList', taskListDirective);

    taskListDirective.$inject = ['dataService', '$log', 'AlertService'];

    function taskListDirective(dataService, $log, AlertService) {

        return {
            restrict: 'E',
            scope: {
                tasks: '='
            },
            templateUrl: 'app/components/task-list/task-list.html',
            link: function (scope, element, attrs) {
                scope.actionButtons = attrs.actionButtons;

                scope.completed = (attrs.completed == 'true');

                scope.pooled = [];

                scope.claimTask = function (taskId, take) {
                    var user;
                    dataService.tasks.claim(taskId, take).then(function (data) {
                        $log.debug(data);
                        scope.pooled[taskId] = user !== undefined;
                        scope.$parent.loadTasks();
                    }, function (err) {
                        $log.error(err);
                        AlertService.error("Richiesta non riuscita<br>" + err.data.message);
                    });
                };
            }
        };
    }
})();