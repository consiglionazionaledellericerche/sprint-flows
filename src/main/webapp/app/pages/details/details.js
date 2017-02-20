(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('details', {
            parent: 'app',
            url: '/details?processInstanceId',
            data: {
                authorities: ['ROLE_USER']
            },
            views: {
                'content@': {
                    templateUrl: 'app/pages/details/details.html',
                    controller: 'DetailsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                mainTranslatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate,$translatePartialLoader) {
//                todo: da risolvere (internazionalizzazione?)
//                    $translatePartialLoader.addPart('details');
                    return $translate.refresh();
                }]
            }
        });
    }
})();
