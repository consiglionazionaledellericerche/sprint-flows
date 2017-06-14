(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('userinput', userinput);

    userinput.$inject = ['dataService', '$log'];

    /**
     * Questa direttiva e' un semplicissimo typeahead con dei parametri preimpostati
     * L'API e' semplicissima: ng-model e ng-required
     */
    function userinput(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/userinput/userinput.html',
            scope: {
              ngModel: '=',
              ngRequired: '@'
            },
            link: function ($scope, element, attrs) {

              $scope.loadUsers = function(filter) {
                return dataService.users(filter)
                .then(function(response) {
                  $scope.hasMore = response.data.more;
                  return response.data.results;
                });
              };
            }
        }
    }
})();