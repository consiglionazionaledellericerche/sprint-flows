(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ManualisticaController', ManualisticaController);

    ManualisticaController.$inject = ['$scope', '$state', 'dataService'];

    function ManualisticaController($scope, $state, dataService) {
        var vm = this;

        dataService.manuali.getElenco().then(function(response) {
            vm.manuali = response.data;
        });

        $scope.downloadManuale = function(manuale) {
            dataService.manuali.getManuale(manuale).then(
                function(response) {
                    //console.log(response.data);
                }
            )
        }

    }

})();