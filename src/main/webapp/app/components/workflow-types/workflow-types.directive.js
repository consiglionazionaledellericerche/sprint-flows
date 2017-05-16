(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('workflowTypes', workflowTypesDirective);

  workflowTypesDirective.$inject = ['$rootScope'];

  function workflowTypesDirective(rootScope) {

    return {
            restrict: 'E',
            scope: {
                processDefinitions: '=',
                filters: '='
            },
            templateUrl: 'app/components/workflow-types/workflow-types.html',
            link: function (scope) {
                scope.selectForm = function(processDefinition) {
                    rootScope.current = processDefinition;

                    if (processDefinition && scope.filters) {
                         scope.formUrl = 'api/forms/'+ processDefinition.key + '/1/search';
                    }
                };
            }
        };
    }
})();