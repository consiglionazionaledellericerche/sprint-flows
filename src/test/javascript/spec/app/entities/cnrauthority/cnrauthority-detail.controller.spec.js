'use strict';

describe('Controller Tests', function() {

    describe('Cnrauthority Management Detail Controller', function() {
        var $scope, $rootScope;
        var MockEntity, MockPreviousState, MockCnrauthority;
        var createController;

        beforeEach(inject(function($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            MockEntity = jasmine.createSpy('MockEntity');
            MockPreviousState = jasmine.createSpy('MockPreviousState');
            MockCnrauthority = jasmine.createSpy('MockCnrauthority');
            

            var locals = {
                '$scope': $scope,
                '$rootScope': $rootScope,
                'entity': MockEntity,
                'previousState': MockPreviousState,
                'Cnrauthority': MockCnrauthority
            };
            createController = function() {
                $injector.get('$controller')("CnrauthorityDetailController", locals);
            };
        }));


        describe('Root Scope Listening', function() {
            it('Unregisters root scope listener upon scope destruction', function() {
                var eventType = 'sprintApp:cnrauthorityUpdate';

                createController();
                expect($rootScope.$$listenerCount[eventType]).toEqual(1);

                $scope.$destroy();
                expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
            });
        });
    });

});
