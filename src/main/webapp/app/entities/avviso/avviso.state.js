(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('avviso', {
            parent: 'entity',
            url: '/avviso?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.avviso.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/avviso/avvisos.html',
                    controller: 'AvvisoController',
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
                    $translatePartialLoader.addPart('avviso');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('avviso-detail', {
            parent: 'entity',
            url: '/avviso/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.avviso.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/avviso/avviso-detail.html',
                    controller: 'AvvisoDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('avviso');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Avviso', function($stateParams, Avviso) {
                    return Avviso.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'avviso',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('avviso-detail.edit', {
            parent: 'avviso-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/avviso/avviso-dialog.html',
                    controller: 'AvvisoDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Avviso', function(Avviso) {
                            return Avviso.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('avviso.new', {
            parent: 'avviso',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/avviso/avviso-dialog.html',
                    controller: 'AvvisoDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                contenuto: null,
                                attivo: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('avviso', null, { reload: 'avviso' });
                }, function() {
                    $state.go('avviso');
                });
            }]
        })
        .state('avviso.edit', {
            parent: 'avviso',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/avviso/avviso-dialog.html',
                    controller: 'AvvisoDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Avviso', function(Avviso) {
                            return Avviso.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('avviso', null, { reload: 'avviso' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('avviso.delete', {
            parent: 'avviso',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/avviso/avviso-delete-dialog.html',
                    controller: 'AvvisoDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Avviso', function(Avviso) {
                            return Avviso.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('avviso', null, { reload: 'avviso' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
