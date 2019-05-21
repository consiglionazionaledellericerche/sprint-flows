(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('activeFlows', {
            parent: 'admin',
            url: '/activeFlows',
            data: {
                authorities: ['ROLE_ADMIN'],
//                todo
                pageTitle: 'logs.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/admin/active-flows/active-flows.html',
                    controller: 'ActiveFlowsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
//                todo: da risolvere (internazionalizzazione?)
//                    $translatePartialLoader.addPart('activeFlows');
                    return $translate.refresh();
                }]
            }
        });
    }
})();
