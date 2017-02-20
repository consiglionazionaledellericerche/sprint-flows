(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('task-details', taskDetailsDirective);

  taskDetailsDirective.$inject = ['dataService', '$sessionStorage', '$log'];

  function taskDetailsDirective(dataService, $sessionStorage, $log) {

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
      templateUrl: 'app/components/task-details/task.details.html',
      link: function (scope, element, attrs) {

        scope.modalTaskMetadata = function (task) {
          $log.info(task);
//          todo: implementare il sercice
          taskService.getTaskMetadata(task.name.replace(':', '_')).then(function (data) {
            var displayData = "", columnValue;
            _.each(data.fields, function (item) {
              try {
                columnValue = task.properties[item.property.replace(':', '_')];
                if (columnValue !== undefined) {
                  displayData += "<p><b>" + i18nService.i18n(item.label) + "</b> " + columnValue + "</p>";
                }
              } catch (err) {
                $log.info(err);
              }
            });
            $log.info(displayData);
            var modalContent = modalService.modal("Dettaglio task: " + task.title, displayData);
            $('<div class="modal fade role="dialog" tabindex="-1"></div>').append(modalContent).modal();
          });
        };
      }
    };
  }
})();