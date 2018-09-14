(function() {
	'use strict';

	angular.module('sprintApp')
		.directive('cnrTree', cnrTree);

	cnrTree.$inject = ['dataService', '$sessionStorage', '$log'];

	function cnrTree(dataService, $sessionStorage, $log) {
		return {
			restrict: 'E',
			scope: {
				siglaList: '@',
				listName: '@',
				cdsuo: '@?',
				label: '@',
				ngModel: '=',
				ngModelid: '='
			},
			templateUrl: 'app/components/cnrtree/cnrtree.html',


			link: function link($scope, elementt, attrs) {

				$scope.treeConfig = {
					version: 1
				};
				$scope.jsonlist = [];
				$scope.ignoreModelChanges = function() {
					return true;
				};

				if ($scope.siglaList) {
					dataService.sigladynamiclist.byName($scope.listName).then(
						function(response) {
							var lists = JSON.parse(response.data.listjson);
							//mapping del json di risposta di SIGLA nel json atteso dal componente cnrtree
							$scope.jsonlist = lists["elements"].map(function(el) {
								return {
									"id": el.codice_anac.split("-")[0],
									"text": el.ds_proc_amm
								};
							});
							$scope.treeConfig.version++;
						},
						function(response) {
							$log.error(response);
						}
					);
				} else {
					dataService.dynamiclist.byName($scope.listName).then(
						function(response) {
							var lists = JSON.parse(response.data.listjson);
							var type = ($scope.cdsuo !== undefined && $scope.cdsuo !== '') ? $scope.cdsuo : 'default';
							$scope.jsonlist = lists[type];
							$scope.treeConfig.version++;
						},
						function(response) {
							$log.error(response);
						}
					);
				}

				$scope.readyCB = function() {
				    $log.info($scope.ngModelid);
				    if ('autofill' in attrs) {

				        if ($scope.ngModelid) {
				            $scope.treeInstance.jstree(true).select_node($scope.ngModelid);

                        } else {
                            var nomeModelId = attrs.ngModelid.split('.').pop();
                            var idDaSelezionare = $scope.$parent.vm.taskVariables[nomeModelId];
                            $scope.treeInstance.jstree(true).select_node(idDaSelezionare);
                        }
				    }
				}

				$scope.select_node = function(discard, selection) {
					if (selection.node.children.length === 0) {
						$scope.ngModel = selection.node.text;
						$scope.ngModelid = selection.node.id;
					} else {
						$scope.treeInstance.jstree(true).deselect_all({});
					}
				};
			}

		};
	}
})();