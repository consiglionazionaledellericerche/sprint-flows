(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ManualisticaController', ManualisticaController);

    ManualisticaController.$inject = ['$scope', '$state', 'dataService', 'utils'];

    function ManualisticaController($scope, $state, dataService, utils) {
        var vm = this;
        //Recupero i Manuali
        dataService.manuali.getElenco().then(function(response) {
            vm.manuali = response.data;
        });
        //Recupero le Faq readable
        dataService.faq.getReadable().then(function(faqs) {
            vm.faqs = faqs.data;
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