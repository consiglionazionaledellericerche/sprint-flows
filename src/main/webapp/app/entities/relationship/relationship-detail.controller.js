(function() {
    'use strict';

    angular
        .module('sprintApp')
        .controller('RelationshipDetailController', RelationshipDetailController);

    RelationshipDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Relationship'];

    function RelationshipDetailController($scope, $rootScope, $stateParams, previousState, entity, Relationship) {
        var vm = this;

        vm.relationship = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sprintApp:relationshipUpdate', function(event, result) {
            vm.relationship = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
