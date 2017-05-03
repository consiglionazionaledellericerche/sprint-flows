(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('TaskController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log', '$http', 'Upload', 'utils'];

    function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log, $http, Upload, utils) {
        var vm = this;
        vm.data = {};

        vm.treeConfig = {
                core : {
                    multiple : false,
                    animation: true,
                    error : function(error) {
                        $log.error('treeCtrl: error from js tree - ' + angular.toJson(error));
                    },
                    check_callback : true,
                    worker : true
                },
                types : {
                    default : {
                        icon : 'glyphicon glyphicon-flash'
                    },
                    star : {
                        icon : 'glyphicon glyphicon-star'
                    },
                    cloud : {
                        icon : 'glyphicon glyphicon-cloud'
                    }
                },
                version : 1,
                plugins : ['types']
    };

        vm.treeModel = [{
            "id": "ajson1",
            "parent": "#",
            "text": "Simple root node"
          }, {
            "id": "ajson2",
            "parent": "#",
            "text": "Root node 2"
          }, {
            "id": "ajson3",
            "parent": "ajson2",
            "text": "Child 1"
          }, {
            "id": "ajson4",
            "parent": "ajson2",
            "text": "Child 2"
          }]

        dataService.dynamiclist.byName('capitolo').then(
          function(response) {
              vm.treeModel = response.data;
          }
        );

        $log.info($state.params.processDefinition);

        if ($state.params.taskId) {
            $log.info("getting task ifno");

            dataService.tasks.getTask($state.params.taskId).then(
                    function(response) {
//                        vm.data = utils.refactoringVariables(response.data).variabili;
                        vm.data.taskId = $state.params.taskId;
                        vm.diagramUrl = '/rest/diagram/taskInstance/'+ response.data.id;
                        var processDefinitionKey = response.data.processDefinitionId.split(":")[0];
                        vm.formUrl = 'api/forms/task/'+ response.data.id;
                    });
        } else {
            vm.data.definitionId = $state.params.processDefinitionId;
            var processDefinitionKey = $state.params.processDefinitionId.split(":")[0];
            var processVersion       = $state.params.processDefinitionId.split(":")[1];
            vm.diagramUrl = "/rest/diagram/processDefinition/" + $state.params.processDefinitionId;
            vm.formUrl = 'api/forms/'+ processDefinitionKey + "/" + processVersion + "/" + $state.params.taskName
        }
        $scope.select_node = function (discard, selection) {console.log("select node");};
        $scope.submitTask = function(file) {

            $log.info(vm);
            $log.info(taskForm);
            if (validate(vm.data)) {

                Upload.upload({
                    url: 'rest/tasks/complete',
                    data: vm.data,
                }).then(function (response) {
//                  $timeout(function () {
//                  file.result = response.data;
//                  });

                    $log.info(response);
                    AlertService.success("Richiesta completata con successo");
                    $state.go('availabletasks');

                }, function (response) {
                    $log.error(err);
                    AlertService.error("Richiesta non riuscita");
                    if (response.status > 0)
                        $scope.errorMsg = response.status + ': ' + response.data;
                });
//              var file = vm.data.documentiPrincipali;
//              var payload = new FormData();
//              payload.append('file', file);
//              for (var key in vm.data) {
//              payload.append(key, vm.data[key]);
//              }
//              dataService.tasks.complete(payload)
//              .then(
//              function(data) {
//              $log.info(data);
//              AlertService.success("Richiesta completata con successo");
//              $state.go('availabletasks');
//              },
//              function(err) {
//              $log.error(err);
//              AlertService.error("Richiesta non riuscita");
//              });
            }
        }

        $scope.reloadImg = function() {
            $log.info(vm.font);
            vm.diagramUrl = '/rest/diagram/taskInstance/'+ $state.params.taskId +'/'+ vm.font +'?' + new Date().getTime();
        }

        function validate(data) {
            $log.debug("validation not implemented yet");
            return true;
        }
    }
})();
