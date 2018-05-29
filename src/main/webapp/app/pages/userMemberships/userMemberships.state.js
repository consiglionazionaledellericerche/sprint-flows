(function() {
	'use strict';

	angular
		.module('sprintApp')
		.config(stateConfig);

	stateConfig.$inject = ['$stateProvider'];

	function stateConfig($stateProvider) {
		$stateProvider
			.state('user-memberships', {
				parent: 'entity',
				url: '/userMemberships?page&sort&search',
				data: {
					authorities: ['ROLE_USER'],
					pageTitle: 'sprintApp.userMemberships.home.title'
				},
				views: {
					'content@': {
						templateUrl: 'app/pages/userMemberships/userMemberships.html',
						controller: 'UserMembershipsController',
						controllerAs: 'vm'
					}
				},
				params: {
					page: {
						value: '1',
						squash: true
					},
					sort: {
						value: 'id,asc',
						squash: true
					},
					search: null
				},
				resolve: {
					pagingParams: ['$stateParams', 'PaginationUtil', function($stateParams, PaginationUtil) {
						return {
							page: PaginationUtil.parsePage($stateParams.page),
							sort: $stateParams.sort,
							predicate: PaginationUtil.parsePredicate($stateParams.sort),
							ascending: PaginationUtil.parseAscending($stateParams.sort),
							search: $stateParams.search
						};
					}],
					translatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
						$translatePartialLoader.addPart('userMemberships');
						$translatePartialLoader.addPart('membership');
						$translatePartialLoader.addPart('global');
						return $translate.refresh();
					}]
				}
			})
			.state('group-memberships', {
				parent: 'app',
				url: '/group-membership?page&sort&search',
				data: {
					authorities: ['ROLE_USER'],
					pageTitle: 'sprintApp.userMemberships.home.title'
				},
				views: {
					'content@': {
						templateUrl: 'app/pages/userMemberships/groupMemberships.html',
						controller: 'GroupMembershipsController',
						controllerAs: 'vm'
					}
				},
				params: {
					page: {
						value: '1',
						squash: true
					},
					sort: {
						value: 'id,asc',
						squash: true
					},
					search: null,
					groupname: null,
					displayName: null
				},
				resolve: {
					modify: [function() {
						return false;
					}],
					pagingParams: ['$stateParams', 'PaginationUtil', function($stateParams, PaginationUtil) {
						return {
							page: PaginationUtil.parsePage($stateParams.page),
							sort: $stateParams.sort,
							predicate: PaginationUtil.parsePredicate($stateParams.sort),
							ascending: PaginationUtil.parseAscending($stateParams.sort),
							search: $stateParams.search
						};
					}],
					translatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
						$translatePartialLoader.addPart('userMemberships');
						$translatePartialLoader.addPart('membership');
						$translatePartialLoader.addPart('global');
						return $translate.refresh();
					}],
					previousState: ["$state", function($state) {
						var currentStateData = {
							name: $state.current.name || 'userMemberships',
							params: $state.params,
							url: $state.href($state.current.name, $state.params)
						};
						return currentStateData;
					}]
				}
			})
			.state('group-memberships-edit', {
				parent: 'app',
				url: '/group-memberships-edit?page&sort&search',
				data: {
					authorities: ['ROLE_USER'],
					pageTitle: 'sprintApp.userMemberships.home.title'
				},
				views: {
					'content@': {
						templateUrl: 'app/pages/userMemberships/groupMemberships.html',
						controller: 'GroupMembershipsController',
						controllerAs: 'vm'
					}
				},
				params: {
					page: {
						value: '1',
						squash: true
					},
					sort: {
						value: 'id,asc',
						squash: true
					},
					search: null,
					groupname: null,
					displayName: null
				},
				resolve: {
					modify: [function() {
						return true;
					}],
					pagingParams: ['$stateParams', 'PaginationUtil', function($stateParams, PaginationUtil) {
						return {
							page: PaginationUtil.parsePage($stateParams.page),
							sort: $stateParams.sort,
							predicate: PaginationUtil.parsePredicate($stateParams.sort),
							ascending: PaginationUtil.parseAscending($stateParams.sort),
							search: $stateParams.search
						};
					}],
					translatePartialLoader: ['$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
						$translatePartialLoader.addPart('userMemberships');
						$translatePartialLoader.addPart('membership');
						$translatePartialLoader.addPart('global');
						return $translate.refresh();
					}],
					previousState: ["$state", function($state) {
						var currentStateData = {
							name: $state.current.name || 'userMemberships',
							params: $state.params,
							url: $state.href($state.current.name, $state.params)
						};
						return currentStateData;
					}]
				}
			})
			.state('userMembershipDelete', {
				parent: 'group-memberships',
				url: '/{id}/delete',
				data: {
					authorities: ['ROLE_USER']
				},
				onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
					$uibModal.open({
						templateUrl: 'app/pages/userMemberships/userMembership-delete-dialog.html',
						controller: 'UserMembershipDeleteController',
						controllerAs: 'vm',
						size: 'md',
						resolve: {
							entity: ['$state', 'Membership', function($state, Membership) {
								return Membership.get({
									id: $stateParams.id
								}).$promise;
							}]
						}
					}).result.then(function() {
						$state.go('group-memberships-edit', {
							groupname: $stateParams.groupname,
							displayName: $stateParams.displayName
						}, {
							reload: 'group-memberships-edit'
						});
					}, function() {
						$state.go('^');
					});
				}],
				params: {
					groupname: null,
					displayName: null
				}
			})
			.state('user-membership-new', {
				parent: 'group-memberships',
				url: '/newMembership/',
				data: {
					authorities: ['ROLE_USER']
				},
				onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
					$uibModal.open({
						templateUrl: 'app/pages/userMemberships/user-membership-new.html',
						controller: 'UserMembershipNewController',
						controllerAs: 'vm',
						backdrop: 'static',
						size: 'lg',
						resolve: {
							entity: ['$stateParams', '$state', function($stateParams, $state) {
								return {
									username: null,
									groupname: $stateParams.groupname,
									grouprole: null,
									id: null
								};
							}]
						}
					}).result.then(function() {
						$state.go('group-memberships-edit', {
							groupname: $stateParams.groupname,
						    displayName: $stateParams.displayName
						}, {
							reload: 'group-memberships-edit'
						});
					}, function() {
						$state.go('^');
					});
				}]
			});
	}
})();