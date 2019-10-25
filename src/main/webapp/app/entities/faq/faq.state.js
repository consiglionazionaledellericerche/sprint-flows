(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('faq', {
            parent: 'entity',
            url: '/faq',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.faq.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/faq/faqs.html',
                    controller: 'FaqController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('faq');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('faq-detail', {
            parent: 'entity',
            url: '/faq/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'sprintApp.faq.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/faq/faq-detail.html',
                    controller: 'FaqDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('faq');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Faq', function($stateParams, Faq) {
                    return Faq.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'faq',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('faq-detail.edit', {
            parent: 'faq-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/faq/faq-dialog.html',
                    controller: 'FaqDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Faq', function(Faq) {
                            return Faq.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('faq.new', {
            parent: 'faq',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/faq/faq-dialog.html',
                    controller: 'FaqDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                domanda: null,
                                risposta: null,
                                isReadable: false,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('faq', null, { reload: 'faq' });
                }, function() {
                    $state.go('faq');
                });
            }]
        })
        .state('faq.edit', {
            parent: 'faq',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/faq/faq-dialog.html',
                    controller: 'FaqDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Faq', function(Faq) {
                            return Faq.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('faq', null, { reload: 'faq' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('faq.delete', {
            parent: 'faq',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/faq/faq-delete-dialog.html',
                    controller: 'FaqDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Faq', function(Faq) {
                            return Faq.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('faq', null, { reload: 'faq' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
