(function() {
    'use strict';

    angular.module('sprintApp')
        .directive('wysiwyg', wysiwyg);

    wysiwyg.$inject = ['dataService', '$log'];

    /**
     * Questa direttiva e' un semplicissimo typeahead con dei parametri preimpostati
     * L'API e' semplicissima: ng-model e ng-required
     */
    function wysiwyg(dataService, $log) {
        return {
            restrict: 'E',
            templateUrl: 'app/inputs/wysiwyg/wysiwyg.html',
            scope: {
                ngModel: '=',
                ngRequired: '@',
                placeholder: '@'
            },
            link: function($scope, element, attrs) {
            	
            	// workaround necessario se ci sono piu' editor sulla stessa pagina
            	// bug: angular non aggiorna le classi css
            	var events = ['trixInitialize', 'trixChange', 'trixSelectionChange', 'trixFocus', 'trixBlur', 'trixFileAccept', 'trixAttachmentAdd', 'trixAttachmentRemove'];
            	for (var i = 0; i < events.length; i++) {
            	    $scope[events[i]] = function(e, editor) {
            	        console.log('Event type:', e.type);
            	    }
            	};
            	
            	
                if ('autofill' in attrs) {
                    var nomeModelId = attrs.ngModel.split('.').pop();
                    $scope.ngModel = $scope.$parent.data.entity.variabili[nomeModelId];
                }   
            }
        };
    }
})();
