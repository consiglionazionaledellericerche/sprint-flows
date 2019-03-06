(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ManualisticaController', ManualisticaController);

    ManualisticaController.$inject = ['$scope', '$state', 'dataService', 'utils'];

    function ManualisticaController($scope, $state, dataService, utils) {
        var vm = this;

        dataService.manuali.getElenco().then(function(response) {
            vm.manuali = response.data;
        });

        $scope.downloadManuale = function(manuale) {

            dataService.manuali.getManuale(manuale).then(
                function(response) {
                    var file = new Blob([response.data], {
                        type: "application/pdf"
                    });
                    saveAs(file, manuale+".pdf");
                }
            )
        }

    }

})();