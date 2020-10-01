(function() {
    'use strict';

    angular
        .module('sprintApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('archivedetails', {
            parent: 'app',
            url: '/archivedetails?processInstanceId&taskId',
            data: {
                authorities: ['ROLE_USER']
            },
            views: {
                'content@': {
                    templateUrl: 'app/pages/archivedetails/archivedetails.html',
                    controller: 'ArchiveDetailsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                mainTranslatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate,$translatePartialLoader) {
//                TODO: da risolvere (internazionalizzazione?)
//                    $translatePartialLoader.addPart('details');
                    return $translate.refresh();
                }]
            }
        });
    }
})();
