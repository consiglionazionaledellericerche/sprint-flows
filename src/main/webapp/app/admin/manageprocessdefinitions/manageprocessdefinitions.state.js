(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('manageprocessdefinitions', {
            parent: 'admin',
            url: '/manageprocessdefinitions',
            data: {
                authorities: ['ROLE_ADMIN'],
//                todo
                pageTitle: 'logs.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/admin/manageprocessdefinitions/manageprocessdefinitions.html',
                    controller: 'ManageProcessDefinitionsController',
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
