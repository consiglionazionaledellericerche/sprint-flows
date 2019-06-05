(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('relationship', {
            parent: 'entity',
            url: '/relationship?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.relationship.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/relationship/relationships.html',
                    controller: 'RelationshipController',
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
                    $translatePartialLoader.addPart('relationship');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('relationship-detail', {
            parent: 'entity',
            url: '/relationship/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.relationship.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/relationship/relationship-detail.html',
                    controller: 'RelationshipDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('relationship');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Relationship', function($stateParams, Relationship) {
                    return Relationship.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'relationship',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('relationship-detail.edit', {
            parent: 'relationship-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/relationship/relationship-dialog.html',
                    controller: 'RelationshipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Relationship', function(Relationship) {
                            return Relationship.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('relationship.new', {
            parent: 'relationship',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/relationship/relationship-dialog.html',
                    controller: 'RelationshipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                groupName: null,
                                groupRelationship: null,
                                groupRole: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('relationship', null, { reload: 'relationship' });
                }, function() {
                    $state.go('relationship');
                });
            }]
        })
        .state('relationship.edit', {
            parent: 'relationship',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/relationship/relationship-dialog.html',
                    controller: 'RelationshipDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Relationship', function(Relationship) {
                            return Relationship.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('relationship', null, { reload: 'relationship' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('relationship.delete', {
            parent: 'relationship',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/relationship/relationship-delete-dialog.html',
                    controller: 'RelationshipDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Relationship', function(Relationship) {
                            return Relationship.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('relationship', null, { reload: 'relationship' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
