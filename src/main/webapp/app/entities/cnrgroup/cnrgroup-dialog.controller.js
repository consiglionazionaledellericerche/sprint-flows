(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('CnrgroupDialogController', CnrgroupDialogController);

    CnrgroupDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Cnrgroup'];

    function CnrgroupDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Cnrgroup) {
        var vm = this;

        vm.cnrgroup = entity;
//        vm.clear = clear;
//        vm.save = save;
        vm.cnrgroups = Cnrgroup.query({
              page: 0,
              size: 100,
              sort: 'ASC'
        });

        vm.checked = function checked(group){
            return this.cnrgroup.parents.filter(function(g){
                   	return g.id == group.id;
                   }).length >0;
        }

        vm.selectedParents = angular.copy(vm.cnrgroup.parents);

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        vm.clear = function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        vm.save = function save () {
            var parents = $('.parentsChecbox:checked').toArray(), parentsJson = [];

            parents.forEach(function(parent){
                parentsJson.push(JSON.parse(parent.dataset.value));
            });
            vm.isSaving = true;
//            $scope.parent = {
//                parents: [vm.cnrgroup.parents[1]]
//            };
            if (vm.cnrgroup.id !== null) {
                vm.cnrgroup.parents = parentsJson;
                Cnrgroup.update(vm.cnrgroup, onSaveSuccess, onSaveError);
            } else {
                Cnrgroup.save(vm.cnrgroup, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:cnrgroupUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
