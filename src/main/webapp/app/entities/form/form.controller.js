(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('FormController', FormController);

    FormController.$inject = ['$scope', '$state', 'Form', 'ParseLinks', 'AlertService', 'pagingParams', 'paginationConstants'];

    function FormController ($scope, $state, Form, ParseLinks, AlertService, pagingParams, paginationConstants) {
        var vm = this;
        
        vm.loadPage = loadPage;
        vm.predicate = pagingParams.predicate;
        vm.reverse = pagingParams.ascending;
        vm.transition = transition;
//        todo: la paginazione non funziona con la ricerca JS(la lascio commentata per riprenderla eventualmente)
//        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.itemsPerPage = 1000;

        loadAll();

        function loadAll () {
            Form.query({
//                page: pagingParams.page - 1,
                size: vm.itemsPerPage,
                sort: sort()
            }, onSuccess, onError);
            function sort() {
                var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
                if (vm.predicate !== 'id') {
                    result.push('id');
                }
                return result;
            }
            function onSuccess(data, headers) {
                vm.links = ParseLinks.parse(headers('link'));
                vm.totalItems = headers('X-Total-Count');
                vm.queryCount = vm.totalItems;
                vm.forms = data;
//                vm.page = pagingParams.page;
            }
            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        function loadPage (page) {
            vm.page = page;
            vm.transition();
        }

        function transition () {
            $state.transitionTo($state.$current, {
                page: vm.page,
                sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
                search: vm.currentSearch
            });
        }
    }
})();
