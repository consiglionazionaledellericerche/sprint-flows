(function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('FormDialogController', FormDialogController);

    FormDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Form', '$sce'];

    function FormDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Form, $sce) {
        var vm = this;

        vm.form = entity;
        vm.clear = clear;
        vm.save = save;
        vm.aceOpts = {  useWrapMode : false,
                showGutter: true,
                theme:'monokai',
                mode: 'html'
        };
        var tidyOpts = {
//            "doctype": "omit",
//            "omit-optional-tags": true,
            "indent": "auto",
            "indent-spaces": 2,
            "quiet": true,
            "tidy-mark": false,
//            "markup": true,
//            "output-xml": false,
//            "output-html": false,
//            "numeric-entities": true,
//            "quote-marks": true,
//            "quote-nbsp": false,
            "show-body-only": "auto",
//            "quote-ampersand": false,
//            "break-before-br": true,
//            "uppercase-tags": false,
//            "uppercase-attributes": false,
//            "drop-font-tags": true,
//            "tidy-mark": false,
//            "wrap": 100
          }
        vm.preview = "";

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.form.id !== null) {
                Form.update(vm.form, onSaveSuccess, onSaveError);
            } else {
                Form.save(vm.form, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sprintApp:formUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

        vm.runTidy = function() {
            vm.form.form = html_beautify(vm.form.form);
            vm.reloadHtml();
        }
        vm.reloadHtml = function() {
            vm.preview = $sce.trustAsHtml(vm.form.form);
        }
    }
})();
