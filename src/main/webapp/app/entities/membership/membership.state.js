(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('membership', {
            parent: 'entity',
            url: '/membership?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.membership.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/membership/memberships.html',
                    controller: 'MembershipController',
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
                    $translatePartialLoader.addPart('membership');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('membership-detail', {
            parent: 'entity',
            url: '/membership/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.membership.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/membership/membership-detail.html',
                    controller: 'MembershipDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('membership');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Membership', function($stateParams, Membership) {
                    return Membership.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'membership',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('membership-detail.edit', {
            parent: 'membership-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/membership/membership-dialog.html',
                    controller: 'MembershipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Membership', function(Membership) {
                            return Membership.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('membership.new', {
            parent: 'membership',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/membership/membership-dialog.html',
                    controller: 'MembershipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                username: null,
                                groupname: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('membership', null, { reload: 'membership' });
                }, function() {
                    $state.go('membership');
                });
            }]
        })
        .state('membership.edit', {
            parent: 'membership',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/membership/membership-dialog.html',
                    controller: 'MembershipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Membership', function(Membership) {
                            return Membership.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('membership', null, { reload: 'membership' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('membership.delete', {
            parent: 'membership',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/membership/membership-delete-dialog.html',
                    controller: 'MembershipDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Membership', function(Membership) {
                            return Membership.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('membership', null, { reload: 'membership' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
