(function() {
	'use strict';

	angular
	.module('sprintApp')
	.controller('NavbarController', NavbarController);

	NavbarController.$inject = ['$rootScope', '$localStorage', '$scope', '$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'SwitchUserService', 'dataService', '$log'];

	function NavbarController($rootScope, $localStorage, $scope, $state, Auth, Principal, ProfileService, LoginService, SwitchUserService, dataService, $log) {
		var vm = this;

		vm.isNavbarCollapsed = true;
		vm.isAuthenticated = Principal.isAuthenticated;
		vm.$localStorage = $localStorage;

		vm.login = login;
		vm.logout = logout;
		vm.switchUser = switchUser;
		vm.cancelSwitchUser = cancelSwitchUser;
		vm.toggleNavbar = toggleNavbar;
		vm.collapseNavbar = collapseNavbar;
		vm.$state = $state;
		$rootScope.wfDefsStatistics = [];

		//in ogni caso questa chiamata viene cachata e non viene richiamata ad ogni caricamento della navbar
		ProfileService.getProfileInfo().then(function(response) {

			$rootScope.inDevelopment = (response.activeProfiles.includes('dev') ? true : false);
			//verifico qual è il profilo spring con cui è stata avviata l'app per caricare il corrispondente banner
			if (response.activeProfiles.includes('cnr'))
			    $rootScope.app = 'cnr';
			else if (response.activeProfiles.includes('oiv'))
			    $rootScope.app = 'oiv';
			else if (response.activeProfiles.includes('iss'))
                $rootScope.app = 'iss';
			else if (response.activeProfiles.includes('showcase'))
			    $rootScope.app = 'showcase';
			else
			    $rootScope.app = 'none';

			vm.swaggerEnabled = response.swaggerEnabled;
		});


		function switchUser() {
			collapseNavbar();
			SwitchUserService.open();
		}

		function cancelSwitchUser() {
			collapseNavbar();
			dataService.authentication.cancelImpersonate().then(function() {
				Principal.authenticate(null);
				Principal.identity(true).then(function(account) {
					$state.reload();
				});
			})
		}

		function login() {
			collapseNavbar();
			LoginService.open();
		}

		function logout() {
			collapseNavbar();
			Auth.logout();
			$state.go('home');
		}

		function toggleNavbar() {
			vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
		}

		function collapseNavbar() {
			vm.isNavbarCollapsed = true;
		}

		function loadAvailableDefinitions() {
			dataService.definitions.all()
			.then(function(response) {
				//lista delle Process Definition che l'utente può avviare
				$rootScope.wfDefsBootable = response.data.bootable;
				//lista di TUTTE le Process Definition
				$rootScope.wfDefsAll = response.data.all;
				$localStorage.wfDefsAll = response.data.all;

				//popolo l'array delle process Definitions di cui l'utente loggato può vedere le statistiche
				$rootScope.wfDefsStatistics = $localStorage.wfDefsAll.filter(function(processDefinition){
					for (var i = 0; i < vm.account.authorities.length; i++){
						var authority = vm.account.authorities[i];
						if(authority.includes('responsabile#') || authority.includes('supervisore#')){
							if(authority.split(/[#@]/)[1] == processDefinition.key ){
								return true;
							}
						}
					}
				})
			}, function(response) {
				$log.error(response);
			});
		}

		loadAvailableDefinitions();
		$scope.$on('authenticationSuccess', function(event, args) {
			$log.info(event);
			$log.info(args);
			loadAvailableDefinitions();
		});

		$scope.$watch(function() {
			return Principal.isAuthenticated();
		}, function() {
			Principal.identity().then(function(account) {
				vm.account = account;
			})
		});
	}
})();