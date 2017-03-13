(function() {
  'use strict';

  angular.module('sprintApp')
  .factory('dataService', Data);


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

      authentication: {
        impersonate: function(username) {
            return $http.get("login/impersonate?impersonate_username="+ username);
        },
        cancelImpersonate: function() {
            return $http.get("/logout/impersonate");
        }
      },
      tasks: {
        myTasks : function() {
          return $http.get('rest/tasks/mytasks');
        },
        myTasksAvailable : function() {
          return $http.get('rest/tasks/availabletasks');
        },
        complete : function(data) {
          return $http.post('rest/tasks/complete', data);
        },
        claim: function (id, take) {
          return $http({
            url: 'rest/tasks/claim/'+ id,
            method: take ? 'PUT' : 'DELETE'
          });
        },
        getTask: function (id) {
            return $http.get('rest/tasks/'+ id);
        },
        searchTask: function (processInstanceId, active, params, order) {
            return $http.post('rest/tasks/search/' + processInstanceId + '?active=' + active + '&order=' + order, params);
        }
      },
      processInstances: {
        byProcessInstanceId : function(processInstanceId) {
            return $http.get('rest/processInstances?processInstanceId=' + processInstanceId);
        },
        getActive: function(processInstanceId) {
            return $http.get('rest/processInstances/active');
        },
        attachments: function(processInstanceId) {
            return $http.get('rest/processInstances/'+ processInstanceId +'/attachments');
        }
      },
      definitions : {
        all : function() {
          return $http.get('rest/processdefinitions/all');
        },
        get: function(id) {
          return $http.get('rest/processdefinitions/', id);
        }
      }
    };
  }
})();