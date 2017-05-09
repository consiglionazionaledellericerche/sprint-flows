(function() {
  'use strict';

  angular.module('sprintApp')
  .factory('dataService', Data);


  Data.$inject = ['$http', '$location', '$rootScope', '$log', '$sessionStorage'];

  function Data ($http, $location, $rootScope, $log, $sessionStorage) {

    var development = $location.$$port === 9000; //GRUNT PORT;

    $rootScope.development = development;


    return {

      authentication: {
        impersonate: function(username) {
            return $http.get("impersonate/start?impersonate_username="+ username);
        },
        cancelImpersonate: function() {
            return $http.get("impersonate/exit");
        }
      },
      tasks: {
        myTasks : function() {
          return $http.get('api/tasks/mytasks');
        },
        myTasksAvailable : function() {
          return $http.get('api/tasks/availabletasks');
        },
        complete : function(data) {
          return $http.post('api/tasks/complete', data);
        },
        claim: function (id, take) {
          return $http({
            url: 'api/tasks/claim/'+ id,
            method: take ? 'PUT' : 'DELETE'
          });
        },
        getTask: function (id) {
            return $http.get('api/tasks/'+ id);
        },
        getTaskCompleted: function (firstResult, maxResults) {
            return $http.get('api/tasks/taskCompleted?firstResult=' + firstResult + '&maxResults=' + maxResults);
        },
        searchTask: function (processInstance, active, params, order, firstResult, maxResults) {
            var processInstaceId;
            if(processInstance !== undefined){
                processInstaceId = processInstance.key;
            } else {
                processInstaceId = 'all';
            }
            return $http.post('api/tasks/search/' + processInstaceId+
                '?active=' + active +
                '&order=' + order +
                '&firstResult=' + firstResult +
                '&maxResults=' + maxResults, params);
        }
      },
      processInstances: {
        byProcessInstanceId : function(processInstanceId) {
            return $http.get('api/processInstances?processInstanceId=' + processInstanceId);
        },
        myProcessInstances:  function(active) {
            return $http.get('api/processInstances/myProcessInstances?active=' + active);
        },
        getProcessInstance: function(active) {
            return $http.get('api/processInstances/getProcessInstances?active=' + active);
        },
        attachments: function(processInstanceId) {
            return $http.get('api/attachments/'+ processInstanceId);
        },
        attachmentHistory: function(processInstaceId, attachmentName) {
            return $http.get('api/attachments/history/'+ processInstaceId +'/'+ attachmentName);
        }
      },
      definitions : {
        all : function() {
          return $http.get('api/processDefinitions/all');
        },
        get: function(id) {
          return $http.get('api/processDefinitions/', id);
        }
      },
      dynamiclist : {
        byName: function(name) {
          return $http.get('api/dynamiclists/byname/'+ name);
        }
      },
      view: function(processid, type) {
        return $http.get('api/views/'+ processid +'/'+ type);
      }
    };
  }
})();