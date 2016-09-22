(function() {
  'use strict';

  angular.module('sprintApp')
  .factory('dataServiceDeprecated', Data);


  Data.$inject = ['$http', '$location', '$rootScope', '$log', '$sessionStorage'];

  function Data ($http, $location, $rootScope, $log, $sessionStorage) {

    var development = $location.$$port === 9000; //GRUNT PORT;
    var proxy = 'proxy';
    var base = 'rest/';

    $rootScope.development = development;

    function ajax (url, settings) {
      var defaults = {
          method: 'GET',
          headers: {
            'X-CNR-Client': 'flowsApp',
            'X-alfresco-ticket': $sessionStorage.ticket
          }
      };

      var conf = _.extend({
        url: base + url
      }, defaults, settings);

      $log.debug(conf);

      return $http(conf);
    }

    return {
      urls: {
        drop: base + 'drop',
        dropupdate: base + 'drop-update',
        proxy: base + proxy + '/',
        content: base + proxy + '/service/api/node/content/workspace/SpacesStore',
        person: base + proxy + '/service/cnr/person/autocomplete-person',
        instances: base + 'instances',
        bulkinfotest: base + 'bulkinfotest',
        metadata: base + 'metadata'
      },
      descendants: function (id) {
        return ajax('descendants', {
          params: {
            id: id
          }
        });
      },
      groupMembers: function (id, format) {
        return ajax('authority/members?id='+ id +'&format='+format);
      },
      search: function (params) {

        var defaultParams = {
            maxItems: 10,
            skipCount: 0,
            fetchCmisObject: false,
            calculateTotalNumItems: false
        };

        return ajax('search', {
          params: _.extend({}, defaultParams, params)
        });
      },
      security: {
        login: function (username, password) {
          return ajax('security/login', {
            method: 'POST',
            data: {
              username: username,
              password: password
            }
          });
        },
        logout: function () {
          return ajax('security/logout', {
            method: 'DELETE'
          });
        }
      },
      common: function () {
        return ajax('common');
      },
      i18n: function () {
        return ajax('i18n');
      },
      bulkInfo: function (key, name, type) {
        return ajax('bulkInfo/view/' + key + '/' + (type || 'form') + '/' + (name || 'default'));
      },
      bulkInfoTest: function (xml) {
        return ajax('bulkInfoTest', {method: 'POST', headers: { "Content-Type": 'application/xml' }, data: xml});
      },
      processinstances: {
        myinstances: function (params) {
          return ajax('processinstances/myinstances', {
            params: params || {}
          });
        },
        startProcessInstance: function (processName, data) {
          return ajax('processinstances/start/'+ processName, {
            method: 'GET',
            data: data
          });
        },
        completeTask: function (taskId, data) {
          return ajax('taskinstances/complete/'+ taskId, {
            method: 'POST',
            data: data
          });
        },
        getTaskInstance: function (params, id) {
          return ajax('taskinstances/'+ id, {
            params: params
          });
        },
        assignTask: function (id, data) {
          return ajax('taskinstances/'+ id, {
            method: 'PUT',
            data: data
          });
        },
        claimTask: function (id) {
          return ajax('taskinstances/claim/'+ id, {
            method: 'PUT'
          });
        },
        unclaimTask: function (id) {
          return ajax('taskinstances/unclaim/'+ id, {
            method: 'PUT'
          });
        },
        processes: function (params) {
          return ajax('processinstances/processes', {
            params: params || {}
          });
        },
        workflowDefinitions: function (definitionId) {
          return ajax('workflowResource/workflowDefinitions', {
            params: {
              definitionId: definitionId
            }
          });
        },
        workflowInstancesById: function (id) {
          return ajax('processinstances/' + id, {
            params: {
              includeTasks: true
            }
          });
        },
        workflowVariableById: function (id) {
          return ajax('processinstances/variables/' + id.substring(id.indexOf('$') + 1))
        },
        taskVariableById: function (id) {
          return ajax('taskinstances/variables/' + id.substring(id.indexOf('$') + 1));
        },
        myTasks: function (params) {
          return ajax('taskinstances/mytasks', {
            params: params || {}
          });
        },
        myTasksAvailable: function (params) {
          return ajax('taskinstances/mytasksavailable', {
            params: params || {}
          });
        },
        processDefinitions: function (where) {
          return ajax('workflowResource/processDefinitions', {
            params: {
              where: where
            }
          });
        },
        deleteWorkflow: function (id) {
          return ajax('processinstances/deleteWorkflow', {
            method: 'DELETE',
            params: {
              workflowId: id.split('$')[1]
            }
          });
        }
        // missioni: {
        //   bulkInfo: function () {
        //     return ajax(proxy + '_nodes', {
        //       params: {
        //         backend: 'missioni',
        //       }
        //     });
        //   }
        // }
      },
      metadata: {
        byNodeRef: function (params) {
          return ajax('metadata', {
            params: params
          });
        },
        byId: function (qname, id) {
          return ajax('metadataById', {
            params: {
              properties: qname,
              assignedByMeWorkflowIds: id
            }
          });
        }
      },
      authority: {
        groups: {
//        todo: da testare la chiamata dall'applicazione
          myGroupsDescendant: function (userId) {
            return ajax('authority/myGroupsDescendant', {
              params: {
                userId: userId
              }
            });
          }
        },
        person: {
//        todo: da testare la chiamata dall'applicazione
          whoami: function () {
            return ajax('authority/whoami');
          }
        },
        incarichi: function() {
          return ajax('authority/incarichi');
        },
        incarica: function(groupName, userName) {
          return ajax('authority/incarica', {
            method: 'PUT',
            data: {
              groupName: groupName,
              userName: userName
            }
          });
        },
        rimuoviIncarico: function(groupName, userName) {
          return ajax('authority/incarica?groupName='+groupName+'&userName='+userName, {
            method: 'DELETE'
          });
        }
      },

      // TODO deprecated
      proxySupervisor: {
        api: {
          workflowInstances: function (definitionName, params) {
            return ajax('processinstances/workflowInstances/' + definitionName, {
              params: {
                skipCount: params["skipCount"],
                maxItems: params["maxItems"],
                where: params["where"]
              }
            });
          },
          workflowInstanceById: function (id, workflowInstanceById) {
            return ajax('processinstances/workflowInstancesById/' + workflowInstanceById + '/' + id);
          },
//        workflowVariableById: function (id, workflowInstanceById) {
//        return ajax(supervisorProxy + '/workflow-variable/' + workflowInstanceById + '/' + id.substring(id.indexOf('$') + 1));
//        nuovo da testare
          workflowVariableById: function (id) {
            return ajax('processinstances/variables/' + id);
          }
        }
      },

      user: function() {
        return ajax('user');
      }
    };
  }
})();