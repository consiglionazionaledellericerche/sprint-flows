(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('taskList', taskListDirective);

    taskListDirective.$inject = ['dataService', '$localStorage', '$state', '$log', 'AlertService'];

    function taskListDirective(dataService, $localStorage, $state, $log, AlertService) {

        return {
            restrict: 'E',
            scope: {
                tasks: '=',
                inFirma: '=',
                gpick: '=' //class dinamica per il bottone di "dettaglio"
            },
            templateUrl: 'app/components/task-list/task-list.html',
            link: function (scope, element, attrs) {

                scope.pooled = [];

                scope.claimTask = function (taskId, take) {
                    var user;
                    dataService.tasks.claim(taskId, take).then(function (data) {
                        $log.debug(data);
                        scope.pooled[taskId] = user !== undefined;
                        scope.$parent.loadAllTasks();
                    }, function (err) {
                        $log.error(err);
                        AlertService.error("Richiesta non riuscita<br>" + err.data.message);
                    });
                };

                scope.inCart = function (id) {
                    return $localStorage.cart && $localStorage.cart.hasOwnProperty(id);
                }

                scope.addToCart = function(task) {
                    $localStorage.cart = $localStorage.cart || {};
                    $localStorage.cart[task.id] = task;
                }

                scope.removeFromCart = function(task) {
                    delete $localStorage.cart[task.id];
                    if (Object.keys($localStorage.cart).length == 0) {
                        delete $localStorage.cart;
                        $state.go('availableTasks')
                    }
                }

            }
        };
    }
})();