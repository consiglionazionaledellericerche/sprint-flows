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
                url: '/memberships',
                data: {
                    authorities: ['ROLE_USER'],
                    pageTitle: 'sprintApp.membership.home.title',
                },
                views: {
                    'content@': {
                        templateUrl: 'app/entities/membership/memberships.html',
                        controller: 'MembershipController',
                    },
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('membership');
                        $translatePartialLoader.addPart('global');
                        return $translate.refresh();
                    }],
                },
            })
            .state('membership.detail', {
                parent: 'entity',
                url: '/membership/{id}',
                data: {
                    authorities: ['ROLE_USER'],
                    pageTitle: 'sprintApp.membership.detail.title',
                },
                views: {
                    'content@': {
                        templateUrl: 'app/entities/membership/membership-detail.html',
                        controller: 'MembershipDetailController',
                    },
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('membership');
                        return $translate.refresh();
                    }],
                    entity: ['$stateParams', 'Membership', function($stateParams, Membership) {
                        return Membership.get({
                            id: $stateParams.id,
                        });
                    }],
                },
            })
            .state('membership.new', {
                parent: 'membership',
                url: '/new',
                data: {
                    authorities: ['ROLE_USER'],
                },
                onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                    $uibModal.open({
                        templateUrl: 'app/entities/membership/membership-dialog.html',
                        controller: 'MembershipDialogController',
                        size: 'lg',
                    }).result.then(function(result) {
                        $state.go('membership', null, {
                            reload: true,
                        });
                    }, function() {
                        $state.go('membership');
                    });
                }],
            })
            .state('membership.delete', {
                parent: 'membership',
                url: '/{id}/delete',
                data: {
                    authorities: ['ROLE_USER'],
                },
                onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                    $uibModal.open({
                        templateUrl: 'app/entities/membership/membership-delete-dialog.html',
                        controller: 'MembershipDeleteController',
                        size: 'md',
                        resolve: {
                            entity: ['Membership', function(Membership) {
                                return Membership.get({
                                    id: $stateParams.id,
                                });
                            }],
                        },
                    }).result.then(function(result) {
                        $state.go('membership', null, {
                            reload: true,
                        });
                    }, function() {
                        $state.go('^');
                    });
                }],
            });
    }
})();
