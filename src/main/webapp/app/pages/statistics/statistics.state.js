(function() {
    'use strict';

    angular
            .module('sprintApp')
            .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('statistics', {
            parent: 'app',
            url: '/statistics',
            data: {
                authorities: ['ROLE_USER'],
            },
            views: {
                'content@': {
                    templateUrl: 'app/pages/statistics/statistics.html',
                    controller: 'StatisticsController',
                    controllerAs: 'vm',
                },
            },
            params: {
                processDefinition: null,
            },
            resolve: {
                mainTranslatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('global');
                        $translatePartialLoader.addPart('statistics');
                        return $translate.refresh();
                    }],
            },
        });
    }
})();
