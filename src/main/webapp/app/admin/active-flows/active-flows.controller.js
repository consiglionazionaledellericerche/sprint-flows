(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ActiveFlowsController', ActiveFlowsController);

    ActiveFlowsController.$inject = ['dataService', '$log'];

    function ActiveFlowsController (dataService, $log) {
        var vm = this;

          dataService.tasks.getActiveTasks()
          .then(function (response) {
              vm.tasks = response.data;
            }, function (response) {
              $log.error(response);
            });
    }
})();
