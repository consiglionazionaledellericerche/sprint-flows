(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('cnrauthority', {
            parent: 'entity',
            url: '/cnrauthority?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.cnrauthority.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/cnrauthority/cnrauthorities.html',
                    controller: 'CnrauthorityController',
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
                    $translatePartialLoader.addPart('cnrauthority');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('cnrauthority-detail', {
            parent: 'entity',
            url: '/cnrauthority/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.cnrauthority.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/cnrauthority/cnrauthority-detail.html',
                    controller: 'CnrauthorityDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('cnrauthority');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Cnrauthority', function($stateParams, Cnrauthority) {
                    return Cnrauthority.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'cnrauthority',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('cnrauthority-detail.edit', {
            parent: 'cnrauthority-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrauthority/cnrauthority-dialog.html',
                    controller: 'CnrauthorityDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Cnrauthority', function(Cnrauthority) {
                            return Cnrauthority.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('cnrauthority.new', {
            parent: 'cnrauthority',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrauthority/cnrauthority-dialog.html',
                    controller: 'CnrauthorityDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                display_name: null,
                                name: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('cnrauthority', null, { reload: 'cnrauthority' });
                }, function() {
                    $state.go('cnrauthority');
                });
            }]
        })
        .state('cnrauthority.edit', {
            parent: 'cnrauthority',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrauthority/cnrauthority-dialog.html',
                    controller: 'CnrauthorityDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Cnrauthority', function(Cnrauthority) {
                            return Cnrauthority.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('cnrauthority', null, { reload: 'cnrauthority' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('cnrauthority.delete', {
            parent: 'cnrauthority',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/cnrauthority/cnrauthority-delete-dialog.html',
                    controller: 'CnrauthorityDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Cnrauthority', function(Cnrauthority) {
                            return Cnrauthority.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('cnrauthority', null, { reload: 'cnrauthority' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
