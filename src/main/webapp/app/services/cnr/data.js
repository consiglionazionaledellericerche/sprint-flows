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
        complete : function(payload) {
          return $http({
              url: 'rest/tasks/complete',
              method: 'POST',
              headers: {"Content-Type": undefined},
              transformRequest: angular.identity,
              data: payload});
        },
        claim: function (id, take) {
          return $http({
            url: 'rest/tasks/claim/'+ id,
            method: take ? 'PUT' : 'DELETE'
          });
        },
        getTask: function (id) {
            return $http.get('rest/tasks/'+ id);
        }
      },
      processInstances: {
        byProcessInstanceId : function(processInstanceId) {
            return $http.get('rest/processInstances?processInstanceId=' + processInstanceId);
        },
        getActives: function(processInstanceId) {
            return $http.get('rest/processInstances/actives');
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
          return $http.get('rest/processdefinitions/'+ id);
        }
      }
    };
  }
})();