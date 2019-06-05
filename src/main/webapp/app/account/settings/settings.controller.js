(function() {
	'use strict';

	angular
		.module('sprintApp')
		.controller('SettingsController', SettingsController);

	SettingsController.$inject = ['User', 'Principal', 'Auth', 'JhiLanguageService', /* 'User',/* "entity",*/ '$translate'];

	function SettingsController(User, Principal, Auth, JhiLanguageService, /* User,entity,*/ $translate) {
		var vm = this;

		vm.error = null;
		vm.save = save;
		//        vm.settingsAccount = entity;
		vm.settingsAccount = null;
		vm.success = null;
		vm.gender = ["F", "M"];

		//precaricamento informazioni utente
		Principal.identity().then(function(account) {
			User.get({
				login: account.login
			}, function(result) {
				vm.settingsAccount = result;
			});
		});


		function save() {
			User.update(vm.settingsAccount,
				function onSaveSuccess() {
					vm.error = null;
					vm.success = 'OK';
				},
				function onSaveError() {
					vm.success = null;
					vm.error = 'ERROR';
				});
		}
	}
})();