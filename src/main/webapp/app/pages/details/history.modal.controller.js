(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('HistoryModalController', HistoryModalController);

    HistoryModalController.$inject = ['$uibModalInstance', 'tasks', 'startTask', 'initiator'];

    function HistoryModalController ($uibModalInstance, tasks, startTask, initiator) {
      var vm = this;

      vm.startTask = startTask;
      vm.tasks = tasks;
      vm.initiator = initiator;
    }
})();