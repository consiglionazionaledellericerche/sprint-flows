(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('ExternalMessageController', ExternalMessageController);

    ExternalMessageController.$inject = ['$scope', '$state', 'ExternalMessage', 'ParseLinks', 'AlertService', 'pagingParams', 'paginationConstants'];

    function ExternalMessageController ($scope, $state, ExternalMessage, ParseLinks, AlertService, pagingParams, paginationConstants) {
        var vm = this;

        vm.searchParams = {};
        vm.loadPage = loadPage;
        vm.predicate = pagingParams.predicate;
        vm.reverse = pagingParams.ascending;
        vm.transition = transition;
        vm.itemsPerPage = paginationConstants.itemsPerPage;

        $scope.loadAll = function () {
            ExternalMessage.query({
                page: pagingParams.page - 1,
                size: vm.itemsPerPage,
                sort: sort(),
                url: vm.searchParams.url,
                payload: vm.searchParams.payload
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
                vm.externalMessages = data;
                vm.page = pagingParams.page;
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

        $scope.loadAll();
    }
})();
