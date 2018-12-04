(function() {
    'use strict';

    angular.module('sprintApp')
        .factory('utils', Utils);

    Utils.$inject = ['$log', '$http'];

    function Utils($log, $http) {

        function swap(entity) {
            entity.variabili = {};
            entity.variables.forEach(function(el) {
                entity.variabili[el.name] = el.valueUrl ? el.valueUrl : el.value;
            });
            return entity;
        }

        return {
            refactoringVariables: function(input) {
                if (Array.isArray(input)) {
                    input.forEach(swap);
                    return input;
                } else {
                    return swap(input);
                }
            },

            downloadFile: function(url, filename, mimetype) {
                $log.info(url);
                $http.get(url, {
                        responseType: 'arraybuffer'
                    })
                    .success(function(data) {
                        var file = new Blob([data], {
                            type: mimetype
                        });
                        $log.info(file, filename);
                        saveAs(file, filename);
                    });
            },
            //todo: viene usato SOLO in active-flows.controller.js: rimuovere la pagina o aggiornare la chiamata
            oldPopulateTaskParams: function (fields) {
                var processParams = [], //alcuni parametri delle ricerche dei task riguardano anche la ProcessInstance
                    taskParams = [];

                fields.forEach(function(field) {
                    var fieldName = field.getAttribute('id').replace('searchField-', ''),
                        appo = {};
                    if (field.value !== "") {
                        appo["key"] = fieldName;
                        appo["value"] = field.value;
                        appo["type"] = field.getAttribute("type");
                        if (field.id.includes("initiator") || field.id.includes("titolo")) {
                            processParams.push(appo);
                        } else {
                            taskParams.push(appo);
                        }
                    }
                });
                return {
                    "processParams": processParams,
                    "taskParams": taskParams
                };
            },
            populateTaskParams: function (searchParams) {
                var processParams = [],
                    taskParams = [];
                if (searchParams) {
                    $.map(searchParams, function (value, key) {
                        if (value){
                            var appo = {};
                            appo.type = value.substr(0, value.indexOf('=') + 1);
                            appo.key = key;
                            appo.value = value.substr(value.indexOf('=') + 1);

                            if (key.includes('initiator') || key.includes('titolo')) {
                                processParams.push(appo);
                            } else {
                                taskParams.push(appo);
                            }
                        }
                    });
                }
                return {
                    'processParams': processParams,
                    'taskParams': taskParams,
                };
            },
            oldPopulateProcessParams: function (fields) {
                var processParams = {};

                fields.forEach(function(field) {
                    var fieldName = field.getAttribute('id').replace('searchField-', '');
                    if (field.value !== "") 
                    	processParams[fieldName] = field.getAttribute("type") +"="+ field.value;
                });
                return processParams;
            },
            populateProcessParams: function (fields) {
              var processParams = [],
                  taskParams = [];
              if (searchParams) {
                  $.map(searchParams, function (value, key) {
                      if (value){
                          var appo = {};
                          appo.type = value.substr(0, value.indexOf('=') + 1);
                          appo.key = key;
                          appo.value = value.substr(value.indexOf('=') + 1);
                          processParams.push(appo);
                      }
                  });
              }
              return {
                  'processParams': processParams,
                  'taskParams': taskParams,
              };
            },
            parseAttachments: function(attachments) {
//                var appo = [];

                for (var attachment in attachments) {
                    for (var metadato in attachments[attachment].metadati) {
                        attachments[attachment][metadato] = attachments[attachment][metadato] || attachments[attachment].metadati[metadato];
                        // TODO martin ricontrollare le date
                        if (metadato.startsWith('data')) {
                            var date = moment(attachments[attachment][metadato]);
                            attachments[attachment][metadato] = date.isValid() ? date.toDate() : attachments[attachment][metadato];
                        }
                    }
                    delete attachments[attachment].bytes;
                    delete attachments[attachment].metadati
//                    appo.push(attachments[attachment]);
                }
                return attachments;
            },
            loadSearchFields: function(processDefinitionKey, isTaskQuery){
                var formUrl = undefined;
                //Di default, al caricamento della pagina, la processDefinitionKey è 'undefined'
                // quindi carico la form per tutte le Process Definitions ('all')
                if (processDefinitionKey === undefined) {
                    processDefinitionKey = 'all';
                }
                if(isTaskQuery) {
                    formUrl = 'api/forms/'+ processDefinitionKey + '/1/search-ti';
                } else {
                    formUrl = 'api/forms/'+ processDefinitionKey + '/1/search-pi';
                }
                return formUrl;
            }
        };
    }
})();