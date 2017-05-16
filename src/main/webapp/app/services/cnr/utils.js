(function() {
    'use strict';

    angular.module('sprintApp')
    .factory('utils', Utils);

    Utils.$inject = ['$log', '$http'];

    function Utils ($log, $http) {

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
            },

            downloadFile : function(url, filename, mimetype) {
                $log.info(url);
                $http.get(url, { responseType: 'arraybuffer' })
                .success(function(data) {
                    var file = new Blob([data], { type: mimetype });
                    $log.info(file, filename);
                    saveAs(file, filename);
                });
            }
        };
    }
})();