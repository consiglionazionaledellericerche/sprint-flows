(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('blacklist', {
            parent: 'entity',
            url: '/blacklist?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'flowsApp.blacklist.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/blacklist/blacklists.html',
                    controller: 'BlacklistController',
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
                    $translatePartialLoader.addPart('blacklist');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('blacklist-detail', {
            parent: 'entity',
            url: '/blacklist/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'flowsApp.blacklist.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/blacklist/blacklist-detail.html',
                    controller: 'BlacklistDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('blacklist');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Blacklist', function($stateParams, Blacklist) {
                    return Blacklist.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'blacklist',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('blacklist-detail.edit', {
            parent: 'blacklist-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/blacklist/blacklist-dialog.html',
                    controller: 'BlacklistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Blacklist', function(Blacklist) {
                            return Blacklist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('blacklist.new', {
            parent: 'blacklist',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/blacklist/blacklist-dialog.html',
                    controller: 'BlacklistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                email: null,
                                processDefinitionKey: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('blacklist', null, { reload: 'blacklist' });
                }, function() {
                    $state.go('blacklist');
                });
            }]
        })
        .state('blacklist.edit', {
            parent: 'blacklist',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/blacklist/blacklist-dialog.html',
                    controller: 'BlacklistDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Blacklist', function(Blacklist) {
                            return Blacklist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('blacklist', null, { reload: 'blacklist' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('blacklist.delete', {
            parent: 'blacklist',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/blacklist/blacklist-delete-dialog.html',
                    controller: 'BlacklistDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Blacklist', function(Blacklist) {
                            return Blacklist.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('blacklist', null, { reload: 'blacklist' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
