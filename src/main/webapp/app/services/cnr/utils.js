(function() {
    'use strict';

    angular.module('sprintApp')
    .factory('utils', Utils);


    function Utils () {

        function swap(entity) {
            entity.variabili = {};
            entity.variables.forEach( function(el){
                entity.variabili[el.name] = el.valueUrl ? el.valueUrl : el.value;
            });
            return entity;
        }

        return {
            refactoringVariables : function(input){
                if (Array.isArray(input)) {
                    input.forEach(swap);
                    return input;
                } else {
                    return swap(input);
                }
            }
        };
    }
})();