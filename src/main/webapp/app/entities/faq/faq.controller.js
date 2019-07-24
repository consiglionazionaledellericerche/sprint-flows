(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FaqController', FaqController);

    FaqController.$inject = ['$scope', '$state', 'Faq'];

    function FaqController ($scope, $state, Faq) {
        var vm = this;
        
        vm.faqs = [];

        loadAll();

        function loadAll() {
            Faq.query(function(result) {
                vm.faqs = result;
            });
        }
    }
})();
