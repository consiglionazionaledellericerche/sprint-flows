(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ActiveFlowsController', ActiveFlowsController);

    ActiveFlowsController.$inject = ['dataService', '$log', 'utils'];

    function ActiveFlowsController (dataService, $log, utils) {
        var vm = this;

          dataService.processInstances.getActives()
          .then(function (response) {
              vm.processesInstances = response;
            }, function (response) {
              $log.error(response);
            });
    }
})();
