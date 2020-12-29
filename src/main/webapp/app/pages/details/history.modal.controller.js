(function() {
    'use strict';

    angular.module('sprintApp')
        .controller('HistoryModalController', HistoryModalController);

    HistoryModalController.$inject = ['processInstanceId', 'startTask', 'initiator', 'dataService', 'utils'];

    function HistoryModalController(processInstanceId, startTask, initiator, dataService, utils) {
        var vm = this;
        vm.data = {};

        vm.startTask = startTask;
        vm.initiator = initiator;

        dataService.processInstances.getHistoryForPi(processInstanceId).then(
            function (response) {
                vm.tasks = response.data;
                //refactoring dopo la chiamata
                vm.tasks.forEach(function (el) {
                    //recupero l'ultimo task (quello ancora da eseguire)
                    if (el.historyTask.endTime === null) {
                        //recupero la fase
                        vm.activeTask = el.historyTask;
                        utils.refactoringVariables(vm.activeTask);

                        vm.data.fase = el.historyTask.name;
                        //recupero il gruppo/l'utente assegnatario del task
                        el.historyIdentityLink.forEach(function (il) {
                            if (il.type === "candidate") {
                                if (il.groupId !== null) {
                                    vm.data.groupCandidate = il.groupId;
                                } else {
                                    vm.data.userCandidate = il.userId;
                                }
                            }
                        });
                    }
                });
            }
        );
    }
})();