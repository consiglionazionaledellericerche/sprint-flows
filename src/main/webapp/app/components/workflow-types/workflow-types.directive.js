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
                scope.$watchGroup(['$root.current'], function() {
                    if (scope.filters && scope.$root.current) {
                        if(scope.$root.isTaskQuery){
                            scope.formUrl = 'api/forms/'+ scope.$root.current.key + '/1/search-ti';
                        } else {
                            scope.formUrl = 'api/forms/'+ scope.$root.current.key + '/1/search-pi';
                        }
                    } else
                        scope.formUrl = null;
                 });
            }
        };
    }
})();