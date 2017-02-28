(function() {
  'use strict';

  angular.module('sprintApp')
  .factory('utils', Utils);


  function Utils () {
    return {
        refactoringVariables : function(entities){
            entities.forEach(function(entity) {
                entity.variables.map( function(el){
                    entity[el.name] = el.value;
                });
                delete entity.variables;
            })
            return entities;
        }
    };
  }
})();