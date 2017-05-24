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
        vm.data.processDefinitionId = $state.params.processDefinitionId;
        var processDefinitionKey = vm.data.processDefinitionId.split(":")[0];
        var processVersion       = vm.data.processDefinitionId.split(":")[1];

        // Ho bisogno di caricare piu' risorse contemporaneamente (form e data);
        // quando sono finite entrambe, autofillo la form
        var formPromise = $q.defer(), dataPromise = $q.defer();
        $scope.autofill = function() {formPromise.resolve(2);};

        $q.all([formPromise.promise, dataPromise.promise])
        .then(function(value) {
            angular.forEach(taskForm, function(el) {
                if (el.attributes.autofill)
                    vm.data[el.id] = vm.taskVariables[el.id];
            });
        });


        if ($state.params.taskId) {
            dataService.tasks.getTask($state.params.taskId).then(
                function(response) {
                    dataPromise.resolve();
                    vm.data.taskId = $state.params.taskId;

                    vm.taskVariables = utils.refactoringVariables(response.data.task).variabili;
                    vm.attachments = response.data.attachments;
                    vm.attachmentsList = response.data.attachmentsList;
                    vm.diagramUrl = '/rest/diagram/taskInstance/'+ vm.data.taskId;
                    vm.formUrl = 'api/forms/task/'+ vm.data.taskId;
            });
        } else {
            dataPromise.reject("");

            vm.diagramUrl = "/rest/diagram/processDefinition/" + $state.params.processDefinitionId;
            vm.formUrl = 'api/forms/'+ processDefinitionKey + "/" + processVersion + "/" + $state.params.taskName
        }

        $scope.submitTask = function(file) {

            $log.info(Object.keys(vm.data));

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

        $scope.downloadFile = function(url, filename, mimetype) {
            utils.downloadFile(url, filename, mimetype);
        }




        $scope.addFileToData = function(files, nameInScope, multiple) {
            $log.info(files);
            vm.data[nameInScope] = files[0];
        }
    }
})();
