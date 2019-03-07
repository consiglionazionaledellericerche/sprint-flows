(function() {
    'use strict';

    angular.module('sprintApp')
    .directive('fileinput', fileinput);

    fileinput.$inject = ['dataService', '$log', 'utils'];

    /**
     *  Questa direttiva collabora in stretto contatto con FlowsAttachmentService
     *  in particolare per quel che riguarda allegati multiple.
     *  Se multiple e' true, verra' renderizzato l'intero insieme degli allegati
     *  Ognuno puo' essere sovrascritto
     *  Inoltre se ne possono aggiungere ulteriori (sempre multiple), che andranno a finire in coda all'array (es. allegati[5])
     *
     *  Per fare sto widget ho fatto una serie di hack non bruttissimi, ma poco manutenibili, cerchiamo di lasciarlo cosi' ~Martin
     */
    function fileinput(dataService, $log, utils) {

        return {
            restrict: 'E',
            templateUrl: 'app/inputs/fileinput/fileinput.html',
            scope: {
                name: '@',                          // diventera' il nome della variabile nel processo (es. decisioneContrattare o allegati)
                label: '@?',                        // la label di un documento predefinito (quella visualizzata, es. Decisione a Contrattare)
                legend: '@?',                       // la legend in caso di allegati multipli (es. Altri Allegati)
                nota: '@?',                         // TODO non ancora implementato
                accept: '@?',                       // tipi di file da prendere come allegati
                multiple: '=?',                     // se ci saranno piu' file
                min: '@?',
                max: '@?',
                cnrRequired: '=?',                  // tendenzialmente true
                metadatiPubblicazione: '=?',        // se visualizzare gli slider per Urp e Trasparenza
                metadatiPubblicazioneUrp: '=?',
                metadatiPubblicazioneTrasparenza: '=?',
                metadatiProtocollo: '=?',           // se visualizzare lo slider per il Protocollo
                pubblicazioneUrp: '=?',             // impostare manualmente il valore, lo slider sara' disabilitato
                pubblicazioneTrasparenza: '=?',     // impostare manualmente il valore, lo slider sara' disabilitato
                protocollo: '=?',                   // impostare manualmente il valore, lo slider sara' disabilitato
                pubblicazioneDisabilitato: '=?',    // disabilitare gli slider di pubblicazione senza impostare i valori
                pubblicazioneUrpDisabilitato: '=?',    // disabilitare gli slider di pubblicazione senza impostare i valori
                pubblicazioneTrasparenzaDisabilitato: '=?',    // disabilitare gli slider di pubblicazione senza impostare i valori
                protocolloDisabilitato: '=?',        // disabilitare gli slider di protocollo senza impostare i valori
                sliderProtocolloDisabilitato: '=?',
                mostraModifica: '=?'                // visualizzare la versione breve (espandibile) in modifica?
            },
            link: function ($scope, element, attrs) {

                $scope.addRow = function() {
                    if ($scope.rows.length < $scope.max)
                        $scope.rows.push({});
                    $scope.$parent.attachments[$scope.name+($scope.rows.length-1)] = {};
                    $scope.$parent.attachments[$scope.name+($scope.rows.length-1)].aggiorna = true;
                    return false;
                };
                $scope.removeRow = function() {
                    if ($scope.rows.length > $scope.min) {
                        $scope.rows.pop();
                        $scope.$parent.attachments[$scope.name+($scope.rows.length)] = undefined;
                    }
                };

                function init() {
                    $scope.attrs = attrs;
                    $scope.popupOpen = false;

                    if ($scope.$parent.attachments === undefined)
                        $scope.$parent.attachments = {};

                    $scope.attachments = $scope.$parent.attachments;
                    $scope.taskId      = $scope.$parent.taskId;

                    // se sono in modifica, reimposto i dati
                    if ($scope.multiple) {
                        $scope.rows = [];
                        for (var el in $scope.$parent.attachments) {
                            if (el.startsWith($scope.name))
                                $scope.rows.push({});
                        }
                        $scope.min = $scope.min || $scope.rows.length || 0;
                        $scope.max = $scope.max || 999;
                        while ($scope.rows.length < $scope.min)
                            $scope.addRow();

                    } else {
                        $scope.rows = [{}];
                        if (!$scope.$parent.attachments[$scope.name]) {
                            $scope.$parent.attachments[$scope.name] = {};
                            $scope.$parent.attachments[$scope.name].aggiorna = true;
                        }
                    }

                    if ($scope.mostraModifica === undefined)
                        $scope.mostraModifica = true;

                }

                init();

                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                };

                $scope.initRow = function (row, index) {
                    row.rowname = $scope.multiple ? $scope.name+index : $scope.name;
                    $scope.$parent.attachments[row.rowname] = $scope.$parent.attachments[row.rowname] || {};
                    $scope.$parent.attachments[row.rowname].name = row.rowname;
                    row.modifica = $scope.$parent.attachments[row.rowname].time !== undefined;

                    if (!row.modifica)  {
                        $scope.$parent.attachments[row.rowname].pubblicazioneUrp = $scope.pubblicazioneUrp;
                        $scope.$parent.attachments[row.rowname].pubblicazioneTrasparenza = $scope.pubblicazioneTrasparenza;
                        $scope.$parent.attachments[row.rowname].protocollo = $scope.protocollo;
                    }
                }

                $scope.onClickProtocollo = function(row) {
                    if (!$scope.$parent.attachments[row.rowname].protocollo) {
                        $scope.$parent.attachments[row.rowname].dataprotocollo = undefined;
                        $scope.$parent.attachments[row.rowname].numeroprotocollo = undefined;
                    }
                }

                $scope.open = function() {
                    $scope.popupOpen = true;
                };
            }
        }
    }
})();