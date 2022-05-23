(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('external-message', {
            parent: 'entity',
            url: '/external-message?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.externalMessage.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/external-message/external-messages.html',
                    controller: 'ExternalMessageController',
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
                    $translatePartialLoader.addPart('externalMessage');
                    $translatePartialLoader.addPart('externalMessageVerb');
                    $translatePartialLoader.addPart('externalMessageStatus');
                    $translatePartialLoader.addPart('externalApplication');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('external-message-detail', {
            parent: 'entity',
            url: '/external-message/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.externalMessage.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/external-message/external-message-detail.html',
                    controller: 'ExternalMessageDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('externalMessage');
                    $translatePartialLoader.addPart('externalMessageVerb');
                    $translatePartialLoader.addPart('externalMessageStatus');
                    $translatePartialLoader.addPart('externalApplication');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'ExternalMessage', function($stateParams, ExternalMessage) {
                    return ExternalMessage.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'external-message',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('external-message-detail.edit', {
            parent: 'external-message-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/external-message/external-message-dialog.html',
                    controller: 'ExternalMessageDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['ExternalMessage', function(ExternalMessage) {
                            return ExternalMessage.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('external-message.new', {
            parent: 'external-message',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/external-message/external-message-dialog.html',
                    controller: 'ExternalMessageDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                url: null,
                                verb: null,
                                payload: null,
                                status: null,
                                retries: null,
                                lastErrorMessage: null,
                                application: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('external-message', null, { reload: 'external-message' });
                }, function() {
                    $state.go('external-message');
                });
            }]
        })
        .state('external-message.edit', {
            parent: 'external-message',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/external-message/external-message-dialog.html',
                    controller: 'ExternalMessageDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['ExternalMessage', function(ExternalMessage) {
                            return ExternalMessage.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('external-message', null, { reload: 'external-message' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('external-message.delete', {
            parent: 'external-message',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/external-message/external-message-delete-dialog.html',
                    controller: 'ExternalMessageDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['ExternalMessage', function(ExternalMessage) {
                            return ExternalMessage.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('external-message', null, { reload: 'external-message' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
