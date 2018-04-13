'use strict';

describe('Controller Tests', function() {

	describe('Membership Detail Controller', function() {
		var $scope, $rootScope;
		var MockEntity, MockMembership, MockCnrgroup, MockJhi_user;
		var createController;

		beforeEach(inject(function($injector) {
			$rootScope = $injector.get('$rootScope');
			$scope = $rootScope.$new();
			MockEntity = jasmine.createSpy('MockEntity');
			MockMembership = jasmine.createSpy('MockMembership');
			MockCnrgroup = jasmine.createSpy('MockCnrgroup');
			MockJhi_user = jasmine.createSpy('MockJhi_user');


			var locals = {
				'$scope': $scope,
				'$rootScope': $rootScope,
				'entity': MockEntity,
				'Membership': MockMembership,
				'Cnrgroup': MockCnrgroup,
				'Jhi_user': MockJhi_user
			};
			createController = function() {
				$injector.get('$controller')("MembershipDetailController", locals);
			};
		}));


		describe('Root Scope Listening', function() {
			it('Unregisters root scope listener upon scope destruction', function() {
				var eventType = 'sprintApp:membershipUpdate';

				createController();
				expect($rootScope.$$listenerCount[eventType]).toEqual(1);

				$scope.$destroy();
				expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
			});
		});
	});
});