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
            require: 'ngModel',
            templateUrl: 'app/inputs/wysiwyg/wysiwyg.html',
            scope: {
                ngModel: '=',
                ngRequired: '@',
                placeholder: '@'
            },
            link: function($scope, element, attrs, ngModel) {
            	
            	var editorElement = element[0].children[1];
            	var editor = editorElement.editor;
            	
                ngModel.$render = function() {
                    if (editor) {
                    	editor.loadHTML(ngModel.$modelValue);
                    }
                    
                    editorElement.addEventListener('trix-change', function() {
                    	ngModel.$setViewValue(editorElement.innerHTML)
                    });
                    
                    editorElement.addEventListener('trix-blur', function() {
                    	if (!editorElement.innerHTML)
                    		element[0].classList.add('ng-touched')
                    });
                };
            	
                if ('autofill' in attrs) {
                    var nomeModelId = attrs.ngModel.split('.').pop();
                    $scope.ngModel = $scope.$parent.data.entity.variabili[nomeModelId];
                }
                if ('placeholder' in attrs) {
                	$(editorElement).attr('placeholder',$scope.placeholder);
                }
            }
        };
    }
})();
