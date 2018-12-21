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

        dataService.definitions.all().then(function(response) {
           vm.procDefs = response.data.data
        });

        $scope.submitProcessDefinition = function(file) {
//            $log.info(Object.keys(vm.data));

            utils.prepareForSubmit($scope.data, $scope.attachments)

            Upload.upload({
                url: 'api/processDefinitions/send',
                data: $scope.data,
            }).then(function (response) {
                $log.info(response);
                AlertService.success("Richiesta completata con successo");

            }, function (err) {
                $log.error(err);
                AlertService.error("Richiesta non riuscita<br>"+ err.data.message);
            });
        }
    }
})();