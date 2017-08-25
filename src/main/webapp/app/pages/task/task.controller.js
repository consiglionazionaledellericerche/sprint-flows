 (function() {
    'use strict';

    angular
    .module('sprintApp')
    .controller('TaskController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', 'AlertService', '$log', '$http', '$q', 'Upload', 'utils'];

    /**
     * Questo e' un po' il cuore di tutta l'applicazione, per questo e' un pochino piu' complicato di altri
     * Innanzitutto c'e' una promise composta che aspetta che sia la form che le variabili siano caricate,
     * e a quel punto valoriza i campi della form ove richiesto con 'autofill'.
     *
     * Al momento dell'invio della form succedono piu' cose un po' complicate per un motivo specifico:
     * per permettere l'atomicita' delle azioni, i files sono trattati come metadati e vengono inviati
     * insieme alla form degli altri metadati, e non in un momento separato come era nella vecchia Scrivania.
     * Per fare questo usiamo form Multipart, e una libreria angular che gestisce l'invio dei file (wrappata nel nostro fileinput)
     * Questo richiede alcuni accorgimenti, perche' possiamo inviare o JSON o Multipart con files
     *
     * Per questo, al momento dell'invio, campi complessi (subform, campi multipli) vengono serializzati in stringhe.
     * Nel fare questo vanno copiati in una variabile nuova, senno l'UI sballa, e filtrati lato server (FlowsTaskResource.extractParameters())
     *
     * NB: non posso usare angular.copy() o altrimenti duplicare i dati per la submit
     *     perche' non vengono gestiti bene gli oggetti del nuovo tipo File/Blob
     *     Sono costretto a inviare il vm.data originale
     *
     * @author mtrycz
     */
    function HomeController ($scope, Principal, LoginService, $state, dataService, AlertService, $log, $http, $q, Upload, utils) {
        var vm = this;
        vm.data = {};
        vm.taskId = $state.params.taskId;
        vm.data.processDefinitionId = $state.params.processDefinitionId;
        vm.processDefinitionKey = vm.data.processDefinitionId.split(":")[0];
        vm.processVersion       = vm.data.processDefinitionId.split(":")[1];
        vm.detailsView = 'api/views/'+ vm.processDefinitionKey +'/detail';

        // Ho bisogno di caricare piu' risorse contemporaneamente (form e data);
        // quando sono finite entrambe, autofillo la form
        var formPromise = $q.defer(), dataPromise = $q.defer();
        $scope.autofill = function() {formPromise.resolve(2);}; // usato nell'html

        $q.all([formPromise.promise, dataPromise.promise])
        .then(function(value) {
            angular.forEach(taskForm, function(el) {
                if (el.attributes.autofill)
                    vm.data[el.id] = vm.taskVariables[el.id];
            });
        });

        if ($state.params.taskId) {
            dataService.tasks.getTask($state.params.taskId).then(
                function(response) {
                    dataPromise.resolve();
                    vm.data.taskId = $state.params.taskId;
                    //visualizzazione dei metadati del task in esecuzione
                    var processDefinitionKey = response.data.task.processDefinitionId.split(":")[0];
                    vm.detailsView = 'api/views/' + processDefinitionKey + '/detail';
                    vm.data.entity = utils.refactoringVariables([response.data.task])[0];

                    vm.taskVariables = utils.refactoringVariables(response.data.task).variabili;
                    vm.attachments = utils.parseAttachments(response.data.attachments);
                    vm.attachmentsList = response.data.attachmentsList;
                    vm.diagramUrl = '/rest/diagram/taskInstance/'+ vm.data.taskId +"?"+ new Date().getTime();
                    vm.formUrl = 'api/forms/task/'+ vm.data.taskId;
            });
        } else {
            dataPromise.reject("");

            vm.diagramUrl = "/rest/diagram/processDefinition/" + $state.params.processDefinitionId +"?"+ new Date().getTime();
            vm.formUrl = 'api/forms/'+ vm.processDefinitionKey + "/" + vm.processVersion + "/" + $state.params.taskName
        }

        $scope.submitTask = function(file) {

            $log.info(Object.keys(vm.data));

            if ($scope.taskForm.$invalid) {
                angular.forEach($scope.taskForm.$error, function (field) {
                    angular.forEach(field, function(errorField){
                        errorField.$setTouched();
                    });
                });
                AlertService.warning("Inserire tutti i valori obbligatori.");
            } else {

              // Serializzo gli oggetti complessi in stringhe
              // E' necessario copiarli in un nuovo campo, senno' angular si incasina
              // Non posso usare angular.copy() perche' ho degli oggetti File non gestiti bene
              angular.forEach(vm.data, function(value, key, obj) {
                if (isObject(value)) {
                  obj[key+"_json"] = JSON.stringify(value);
                }
              });

                Upload.upload({
                    url: 'api/tasks/complete',
                    data: vm.data,
                }).then(function (response) {

                    $log.info(response);
                    AlertService.success("Richiesta completata con successo");
                    $state.go('availabletasks');

                }, function (err) {
                    $log.error(err);
                    AlertService.error("Richiesta non riuscita<br>"+ err.data.message);
                });
            }
        }

        $scope.downloadFile = function(url, filename, mimetype) {
            utils.downloadFile(url, filename, mimetype);
        }

        function isObject(value) {
          if (value === null ||
              typeof value !== 'object' ||
              Object.prototype.toString.call(value) === "[object File]" ||
              (Object.prototype.toString.call(value) === "[object Array]" && value.length > 0 && Object.prototype.toString.call(value[0]) === "[object File]") )
            return false;

          return true;
        }
    }
})();
