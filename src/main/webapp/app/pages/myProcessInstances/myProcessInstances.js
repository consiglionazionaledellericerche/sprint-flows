(function () {
    'use strict';

    angular
            .module('sprintApp')
            .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('myProcessInstances', {
            parent: 'app',
            url: '/my-process-instances',
            data: {
                authorities: ['ROLE_USER']
            },
            views: {
                'content@': {
                     templateUrl: 'app/pages/myProcessInstances/myProcessInstances.html',
                    controller: 'MyPIController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                mainTranslatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('global');
                        return $translate.refresh();
                    }]
            }
        });
    }
})();
