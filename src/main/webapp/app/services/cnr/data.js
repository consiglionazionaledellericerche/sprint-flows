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
      tasks: {
        myTasks : function() {
          return $http.get('rest/tasks/mytasks');
        },
        myTasksAvailable : function() {
          return $http.get('rest/tasks/mytasksavailable');
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