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
				stringList: '@',
				cdsuo: '@?',
				label: '@',
				ngModel: '=',
				ngModelid: '='
			},
			templateUrl: 'app/components/cnrtree/cnrtree.html',


			link: function link($scope, elementt, attrs) {

				$scope.treeConfig = {
					version: 1,
					core: {
					    themes : {
					        name: "default"
					    }
					}
				};
				$scope.jsonlist = [];
				$scope.ignoreModelChanges = function() {
					return true;
				};

                    function listToTree(list) {
                        var map = {}, node, roots = [], i;
                        for (i = 0; i < list.length; i += 1) {
                            map[list[i].id] = i; // initialize the map
                            list[i].children = []; // initialize the children
                        }
                        for (i = 0; i < list.length; i += 1) {
                            node = list[i];
                            node.text = list[i].descrizione;
                            if (node.idPadre !== 1) {
                                // if you have dangling branches check that map[node.parentId] exists
                                list[map[node.idPadre]].children.push(node);
                            } else {
                                roots.push(node);
                            }
                        }
                        return roots;
                    }


				 if ($scope.stringList) {
				 	dataService.oil.byCategory(56).then(
				 		function (response) {
				 			$scope.jsonlist = listToTree(response.data);
                            $scope.$parent.vm.taskVariables = $scope.jsonlist;
				 			$scope.treeConfig.version++;
				 		},
				 		function (response) {
				 			$log.error(response);
				 		}
				 	);
				 } else {
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
				}

				$scope.readyCB = function() {

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