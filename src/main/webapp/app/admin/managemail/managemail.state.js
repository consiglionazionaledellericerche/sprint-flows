(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('managemail', {
            parent: 'admin',
            url: '/managemail',
            data: {
                authorities: ['ROLE_ADMIN'],
//                todo
                pageTitle: 'logs.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/admin/managemail/managemail.html',
                    controller: 'ManageMailController',
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
