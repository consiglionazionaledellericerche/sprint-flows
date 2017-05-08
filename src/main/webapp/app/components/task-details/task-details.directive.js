(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('taskDetails', taskDetailsDirective);

  taskDetailsDirective.$inject = ['dataService', '$sessionStorage', '$uibModal', '$log'];

  function taskDetailsDirective(dataService, $sessionStorage, $uibModal, $log) {

    return {
      restrict: 'E',
      scope: {
        task: '='
      },
      templateUrl: 'app/components/task-details/task-details.html',
      link: function (scope) {

        scope.modalTaskMetadata = function (task) {
          $log.info(task);
            $uibModal.open({
                templateUrl: 'app/components/task-details/detailModal.html',
                controllerAs: 'vm',
                controller: 'DetailModalController',
                size: 'md',
                resolve: {
                    task: function() {
                        return task;
                    }
                }
            });
        };
      }
    };
  }
})();