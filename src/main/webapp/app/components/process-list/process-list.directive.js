(function() {
  'use strict';

  angular.module('sprintApp')
  .directive('processList', processListDirective);

  processListDirective.$inject = [];

  function processListDirective() {

    return {
          restrict: 'E',
          scope: {
            processes: '=',
            paging: '='
          },
          templateUrl: 'app/components/process-list/process-list.html',
          link: function (scope) {
            scope.$watch('processes', function(processes) {
                if(processes){
                    scope.processes = processes;
                    scope.variables = [];
                    processes.forEach( function(process){
                        if(process.processVariables){
                            process.variables = process.processVariables;
                            //serve per le HistoricProcessInstance che hanno il valore dell'id del processo in processInstanceId
                            process.id = process.processInstanceId;
                        }else{
                        // serve per la visualizzazione di ProcessInstance che hanno le variabili in forma NON standardizzata
                        //(ad es. nella pagina "activeFlows" {"name":"initiator","type":"string","value":"admin","scope":"local"})
                            var appo = process.variables;
                            process.variables = {};
                            appo.map( function(el){
                                process.variables[el.name] = el.value;
                            });
                        }
                    });
                }
            })
          }
        };
    }
})();