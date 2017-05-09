(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('TaskController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log', '$http', '$q', 'Upload', 'utils'];

    function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log, $http, $q, Upload, utils) {
        var vm = this;
        vm.data = {};
        vm.taskId = $state.params.taskId;
        var formPromise = $q.defer(), dataPromise = $q.defer();
        $log.info(formPromise);
        $log.info(dataPromise);

        // Ho bisogno di caricare piu' risorse contemporaneamente (form e data);
        // quando sono finite entrambe, autofillo la form
        $q.all([formPromise.promise, dataPromise.promise]).then( function(value) {
            angular.forEach(taskForm, function(el) {
                if (el.attributes.autofill)
                    vm.data[el.id] = vm.taskVariables[el.id];
            });
        }, function(err) {
            $log.error(err);
        })

        if ($state.params.taskId) {
            dataService.tasks.getTask($state.params.taskId).then(
                    function(response) {
                        dataPromise.resolve();
                        vm.taskVariables = utils.refactoringVariables(response.data).variabili;
//                        vm.data = utils.refactoringVariables(response.data).variabili;
                        vm.data.taskId = $state.params.taskId;
                        vm.diagramUrl = '/rest/diagram/taskInstance/'+ response.data.id;
                        var processDefinitionKey = response.data.processDefinitionId.split(":")[0];
                        vm.formUrl = 'api/forms/task/'+ response.data.id;
                    });
        } else {
            dataPromise.reject();
            vm.data.definitionId = $state.params.processDefinitionId;
            var processDefinitionKey = $state.params.processDefinitionId.split(":")[0];
            var processVersion       = $state.params.processDefinitionId.split(":")[1];
            vm.diagramUrl = "/rest/diagram/processDefinition/" + $state.params.processDefinitionId;
            vm.formUrl = 'api/forms/'+ processDefinitionKey + "/" + processVersion + "/" + $state.params.taskName
        }

        $scope.select_node = function (discard, selection) {console.log("select node");};

        $scope.submitTask = function(file) {
            $log.info(vm);
            if (validate(vm.data)) {

                Upload.upload({
                    url: 'api/tasks/complete',
                    data: vm.data,
                }).then(function (response) {

                    $log.info(response);
                    AlertService.success("Richiesta completata con successo");
                    $state.go('availabletasks');

                }, function (response) {
                    $log.error(err);
                    AlertService.error("Richiesta non riuscita");
                    if (response.status > 0)
                        $scope.errorMsg = response.status + ': ' + response.data;
                });
            }
        }

        $scope.reloadImg = function() {
            $log.info(vm.font);
            vm.diagramUrl = '/rest/diagram/taskInstance/'+ $state.params.taskId +'/'+ vm.font +'?' + new Date().getTime();
        }

        $scope.autofill = function() {
            formPromise.resolve(2);
        }

        function validate(data) {
            $log.debug("validation not implemented yet");
            return true;
        }
    }
})();
