(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('workflowTypes', workflowTypesDirective);

  workflowTypesDirective.$inject = ['$rootScope', 'dataService', 'Form', '$sessionStorage', '$log'];

  function workflowTypesDirective(rootScope) {

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
            link: function (scope) {
                scope.selectForm = function(processDefinition) {
                    rootScope.current = processDefinition;

                    if (processDefinition) {
                         scope.formUrl = 'api/forms/'+ processDefinition.key + '/1/search';
                    }
                };
            }
        };
    }
})();