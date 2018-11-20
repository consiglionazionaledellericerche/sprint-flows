(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('fileinput', fileinput);

    fileinput.$inject = ['dataService', '$log'];

    /**
     *  Questa direttiva collabora in stretto contatto con FlowsAttachmentService
     *  in particolare per quel che riguarda allegati multiple.
     *  Se multiple e' true, verra' renderizzato l'intero insieme degli allegati
     *  Ognuno puo' essere sovrascritto
     *  Inoltre se ne possono aggiungere ulteriori (sempre multiple), che andranno a finire in coda all'array (es. allegati[5])
     *
     *  Per fare sto widget ho fatto una serie di hack non bruttissimi, ma poco manutenibili, cerchiamo di lasciarlo cosi' ~Martin
     */
    function fileinput(dataService, $log) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/fileinput/fileinput.html',
            scope: {
                pubblicazioneUrp: '@?',
                pubblicazioneTrasparenza: '@?',
                protocollo: '@?',
                metadatiPubblicazione: '@?',
                metadatiProtocollo: '@?',
                multiple: '@?',
                name: '@?',
                label: '@?',
                legend: '@?'
            },
            link: function ($scope, element, attrs) {

                function init() {
                    $scope.min = $scope.min || 0;
                    $scope.max = $scope.max || 999;

                    $scope.data = $scope.$parent.data;

                    $scope.metadatiPubblicazione = ($scope.metadatiPubblicazione == 'true');
                    $scope.pubblicazioneUrp = ($scope.pubblicazioneUrp == 'true');
                    $scope.pubblicazioneTrasparenza = ($scope.pubblicazioneTrasparenza == 'true');
                    $scope.metadatiProtocollo = ($scope.metadatiProtocollo == 'true');
                    $scope.protocollo = ($scope.protocollo == 'true');

                    $scope.scope = $scope;
                    $scope.attrs = attrs;

                    $scope.rows = $scope.name ? [{}] : [];

                    if ($scope.$parent.attachments === undefined)
                        $scope.$parent.attachments = [];

                    if ($scope.multiple == 'false')
                        $scope.document = $scope.$parent.attachments.find(function(el) {
                            return el.name === $scope.attrs.name;
                        })
                 }

                init();

                $scope.filterNames = function(value) {
                    var reg = "^"+$scope.attrs.name+"\\[\\d+\\]";
                    var tester = new RegExp(reg, 'g');
                    return tester.test(value.name);
                }

                $scope.addRow = function() {
                    if ($scope.rows.length < $scope.max)
                        $scope.rows.push({});
                    return false;
                };
                $scope.removeRow = function() {
                    if ($scope.rows.length > $scope.min)
                        $scope.rows.pop();

                    // elimino i dati dell'ultimo alloegato aggiunto
                    var name = 'allegato'+$scope.rows.length;
                    delete $scope.$parent.data[name];
                    delete $scope.$parent.data[name+'_name'];
                    delete $scope.$parent.data[name+'_pubblicazioneUrp'];
                    delete $scope.$parent.data[name+'_pubblicazioneTrasparenza'];
                    delete $scope.$parent.data[name+'_protocollo'];
                    delete $scope.$parent.data[name+'_dataprotocollo'];
                    delete $scope.$parent.data[name+'_numeroprotocollo'];
                    return false;
                };

            }
        }
    }
})();