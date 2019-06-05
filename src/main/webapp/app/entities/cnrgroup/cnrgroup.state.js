(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('cnrgroup', {
            parent: 'entity',
            url: '/cnrgroup?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.cnrgroup.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/cnrgroup/cnrgroups.html',
                    controller: 'CnrgroupController',
                    controllerAs: 'vm'
                }
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'id,asc',
                    squash: true
                },
                search: null
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtil', function ($stateParams, PaginationUtil) {
                    return {
                        page: PaginationUtil.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtil.parsePredicate($stateParams.sort),
                        ascending: PaginationUtil.parseAscending($stateParams.sort),
                        search: $stateParams.search
                    };
                }],
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('cnrgroup');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('cnrgroup-detail', {
            parent: 'entity',
            url: '/cnrgroup/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.cnrgroup.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/cnrgroup/cnrgroup-detail.html',
                    controller: 'CnrgroupDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('cnrgroup');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Cnrgroup', function($stateParams, Cnrgroup) {
                    return Cnrgroup.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'cnrgroup',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('cnrgroup-detail.edit', {
            parent: 'cnrgroup-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrgroup/cnrgroup-dialog.html',
                    controller: 'CnrgroupDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Cnrgroup', function(Cnrgroup) {
                            return Cnrgroup.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('cnrgroup.new', {
            parent: 'cnrgroup',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrgroup/cnrgroup-dialog.html',
                    controller: 'CnrgroupDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                displayName: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('cnrgroup', null, { reload: 'cnrgroup' });
                }, function() {
                    $state.go('cnrgroup');
                });
            }]
        })
        .state('cnrgroup.edit', {
            parent: 'cnrgroup',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrgroup/cnrgroup-dialog.html',
                    controller: 'CnrgroupDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Cnrgroup', function(Cnrgroup) {
                            return Cnrgroup.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('cnrgroup', null, { reload: 'cnrgroup' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('cnrgroup.delete', {
            parent: 'cnrgroup',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrgroup/cnrgroup-delete-dialog.html',
                    controller: 'CnrgroupDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Cnrgroup', function(Cnrgroup) {
                            return Cnrgroup.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('cnrgroup', null, { reload: 'cnrgroup' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
