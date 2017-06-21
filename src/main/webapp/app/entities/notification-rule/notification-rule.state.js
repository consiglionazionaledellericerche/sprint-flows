(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('notification-rule', {
            parent: 'entity',
            url: '/notification-rule?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.notificationRule.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/notification-rule/notification-rules.html',
                    controller: 'NotificationRuleController',
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
                    $translatePartialLoader.addPart('notificationRule');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('notification-rule-detail', {
            parent: 'entity',
            url: '/notification-rule/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.notificationRule.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/notification-rule/notification-rule-detail.html',
                    controller: 'NotificationRuleDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('notificationRule');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'NotificationRule', function($stateParams, NotificationRule) {
                    return NotificationRule.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'notification-rule',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('notification-rule-detail.edit', {
            parent: 'notification-rule-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/notification-rule/notification-rule-dialog.html',
                    controller: 'NotificationRuleDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['NotificationRule', function(NotificationRule) {
                            return NotificationRule.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('notification-rule.new', {
            parent: 'notification-rule',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/notification-rule/notification-rule-dialog.html',
                    controller: 'NotificationRuleDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                processId: null,
                                taskName: null,
                                groups: null,
                                eventType: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('notification-rule', null, { reload: 'notification-rule' });
                }, function() {
                    $state.go('notification-rule');
                });
            }]
        })
        .state('notification-rule.edit', {
            parent: 'notification-rule',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/notification-rule/notification-rule-dialog.html',
                    controller: 'NotificationRuleDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['NotificationRule', function(NotificationRule) {
                            return NotificationRule.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('notification-rule', null, { reload: 'notification-rule' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('notification-rule.delete', {
            parent: 'notification-rule',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/notification-rule/notification-rule-delete-dialog.html',
                    controller: 'NotificationRuleDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['NotificationRule', function(NotificationRule) {
                            return NotificationRule.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('notification-rule', null, { reload: 'notification-rule' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
