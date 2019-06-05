(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('HistoryModalController', HistoryModalController);

    HistoryModalController.$inject = ['$uibModalInstance', 'tasks', 'startTask'];

    function HistoryModalController ($uibModalInstance, tasks, startTask) {
      var vm = this;

      vm.startTask = startTask;
      vm.tasks = tasks;
    }
})();