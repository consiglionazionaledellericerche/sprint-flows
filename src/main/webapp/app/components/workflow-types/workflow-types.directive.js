(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('workflowTypes', workflowTypesDirective);

  workflowTypesDirective.$inject = ['$rootScope', 'dataService', 'Form', '$sessionStorage', '$log'];

  function workflowTypesDirective(rootScope, dataService, Form, $sessionStorage, $log) {

    return {
            restrict: 'E',
            scope: {
                criteria: '=',
                filters: '=',
                resetFilters: '=',
                selectProcessDefinitionKey: '=',
                processDefinitions: '=',
                changed: '='
            },
            templateUrl: 'app/components/workflow-types/workflow-types.html',
            link: function (scope, element, attrs) {
                var vm = this,
                    processVersion = '1.0';

                scope.selectForm = function(processDefinition) {
                    rootScope.current = processDefinition;

                    if (processDefinition) {
                         scope.formUrl = 'api/forms/'+ processDefinition.key + "/" + processVersion + "/search";
                    }
                    scope.criteria.changed = true;
                };
            }
        };
    }
})();