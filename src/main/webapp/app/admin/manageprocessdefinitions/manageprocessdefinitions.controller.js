(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ManageProcessDefinitionsController', ManageProcessDefinitionsController);

    ManageProcessDefinitionsController.$inject = ['$scope', 'paginationConstants', 'dataService', 'utils', '$log', 'Upload', 'AlertService'];

    function ManageProcessDefinitionsController($scope, paginationConstants, dataService, utils, $log, Upload, AlertService) {
        var vm = this;

        $scope.data = {};
        $scope.attachments = {};

        load();
        
        $scope.suspend = function(key) {
            dataService.definitions.suspend(key).then(success, error);
        }
        
        $scope.activate = function(key) {
            dataService.definitions.activate(key).then(success, error);
        }

        function success(response) {
            $log.info(response);
            AlertService.success("Richiesta completata con successo");
            $scope.data.procDef = undefined;
            load();
        }
        
        function error(err) {
            $log.error(err);
            AlertService.error("Richiesta non riuscita<br>"+ err.data.message);
        }

        function load() {
            dataService.definitions.all(true).then(function(response) {
               vm.procDefs = response.data.all;
            });
        }
        

        $scope.submitProcessDefinition = function(file) {

            utils.prepareForSubmit($scope.data, $scope.attachments)

            Upload.upload({
                url: 'api/processDefinitions/send',
                data: $scope.data,
            }).then(success, error);
        }
    }
})();