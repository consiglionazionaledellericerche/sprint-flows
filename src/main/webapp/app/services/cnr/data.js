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
        searchTask: function (processInstance, active, params, order, firstResult, maxResults) {
            var processInstaceId;
            if(processInstance !== undefined){
                processInstaceId = processInstance.key;
            } else {
                processInstaceId = 'all';
            }
            return $http.post('rest/tasks/search/' + processInstaceId+
                '?active=' + active +
                '&order=' + order +
                '&firstResult=' + firstResult +
                '&maxResults=' + maxResults, params);
        }
      },
      processInstances: {
        byProcessInstanceId : function(processInstanceId) {
            return $http.get('rest/processInstances?processInstanceId=' + processInstanceId);
        },
        getActive: function() {
            return $http.get('rest/processInstances/active');
        },
        getCompleted: function() {
            return $http.get('rest/processInstances/completed');
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
          return $http.get('rest/processdefinitions/all');
        },
        get: function(id) {
          return $http.get('rest/processdefinitions/', id);
        }
      }
    };
  }
})();