(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('TaskController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log', '$http', 'Upload', 'utils'];

    function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log, $http, Upload, utils) {
        var vm = this;
        vm.data = {};

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
