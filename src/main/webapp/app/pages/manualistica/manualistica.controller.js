(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ManualisticaController', ManualisticaController);

    ManualisticaController.$inject = ['$scope', '$state'];

    function ManualisticaController($scope, $state) {
        var vm = this;
    }

})();