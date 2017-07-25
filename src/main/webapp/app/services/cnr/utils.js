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
            populateTaskParams: function(fields) {
                var processParams = [], //alcuni parametri delle ricerche dei task riguardano anche la ProcessInstance
                    taskParams = [];

                fields.forEach(function(field) {
                    var fieldName = field.getAttribute('id').replace('searchField-', ''),
                        appo = {};
                    if (field.value !== "") {
                        appo["key"] = fieldName;
                        appo["value"] = field.value;
                        appo["type"] = field.getAttribute("type");
                        if (field.id.includes("initiator") || field.id.includes("titoloIstanzaFlusso")) {
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
            populateProcessParams: function(fields) {
                var processParams = [];

                fields.forEach(function(field) {
                    var fieldName = field.getAttribute('id').replace('searchField-', ''),
                        appo = {};
                    if (field.value !== "") {
                        appo["key"] = fieldName;
                        appo["value"] = field.value;
                        appo["type"] = field.getAttribute("type");
                        processParams.push(appo);
                    }
                });
                return {
                    "processParams": processParams
                };
            },
            parseAttachments: function(attachments) {
                if (attachments) {
                    var appo = [];
                    for (var attachment in attachments) {
                        delete attachments[attachment].bytes;
                        appo.push(attachments[attachment]);
                    }
                    return appo;
                }
            }
        };
    }
})();