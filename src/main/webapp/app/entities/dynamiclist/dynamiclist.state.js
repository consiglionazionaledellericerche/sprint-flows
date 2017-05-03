(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('dynamiclist', {
            parent: 'entity',
            url: '/dynamiclist?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.dynamiclist.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/dynamiclist/dynamiclists.html',
                    controller: 'DynamiclistController',
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
                    $translatePartialLoader.addPart('dynamiclist');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('dynamiclist-detail', {
            parent: 'entity',
            url: '/dynamiclist/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.dynamiclist.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/dynamiclist/dynamiclist-detail.html',
                    controller: 'DynamiclistDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('dynamiclist');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Dynamiclist', function($stateParams, Dynamiclist) {
                    return Dynamiclist.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'dynamiclist',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('dynamiclist-detail.edit', {
            parent: 'dynamiclist-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/dynamiclist/dynamiclist-dialog.html',
                    controller: 'DynamiclistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Dynamiclist', function(Dynamiclist) {
                            return Dynamiclist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('dynamiclist.new', {
            parent: 'dynamiclist',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/dynamiclist/dynamiclist-dialog.html',
                    controller: 'DynamiclistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                listjson: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('dynamiclist', null, { reload: 'dynamiclist' });
                }, function() {
                    $state.go('dynamiclist');
                });
            }]
        })
        .state('dynamiclist.edit', {
            parent: 'dynamiclist',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/dynamiclist/dynamiclist-dialog.html',
                    controller: 'DynamiclistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Dynamiclist', function(Dynamiclist) {
                            return Dynamiclist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('dynamiclist', null, { reload: 'dynamiclist' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('dynamiclist.delete', {
            parent: 'dynamiclist',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/dynamiclist/dynamiclist-delete-dialog.html',
                    controller: 'DynamiclistDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Dynamiclist', function(Dynamiclist) {
                            return Dynamiclist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('dynamiclist', null, { reload: 'dynamiclist' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
