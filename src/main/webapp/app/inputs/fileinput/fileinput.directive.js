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
                name: '@',
                label: '@?',
                legend: '@?',
                note: '@?',
                accept: '@?',
                multiple: '=?',
                cnrRequired: '=?',
                metadatiPubblicazione: '=?',
                metadatiProtocollo: '=?',
                pubblicazioneUrp: '=?',
                pubblicazioneTrasparenza: '=?',
                protocollo: '=?',
                metadatiDisabilitati: '=?'
            },
            link: function ($scope, element, attrs) {

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
                    } else {
                        $scope.rows = [{}]
                    }

                    $scope.min = $scope.min || $scope.rows.length || 0;
                    $scope.max = $scope.max || 999;

                    $scope.metadatiPubblicazione = $scope.metadatiPubblicazione || $scope.pubblicazioneTrasparenza || $scope.pubblicazioneUrp;
                    $scope.metadatiProtocollo = $scope.metadatiProtocollo || $scope.protocollo;

                    $scope.pubblicazioneUrpDisabled = attrs.pubblicazioneUrp !== undefined;
                    $scope.pubblicazioneTrasparenzaDisabled = attrs.pubblicazioneTrasparenza !== undefined;
                    $scope.protocolloDisabled = attrs.protocollo !== undefined;

                }

                init();

                $scope.addRow = function() {
                    if ($scope.rows.length < $scope.max)
                        $scope.rows.push({});
                    return false;
                };
                $scope.removeRow = function() {
                    if ($scope.rows.length > $scope.min) {
                        $scope.rows.pop();
                        $scope.$parent.attachments[$scope.name+($scope.rows.length)] = undefined;
                    }
                };

                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                };

                $scope.initRow = function (row, index) {
                    row.rowname = $scope.multiple ? $scope.name+index : $scope.name;
                    $scope.$parent.attachments[row.rowname] = $scope.$parent.attachments[row.rowname] || {};
                    $scope.$parent.attachments[row.rowname].name = row.rowname;
                    row.modifica = $scope.$parent.attachments[row.rowname].time !== undefined;

                    if (row.modifica) {

                    } else {
                        $scope.$parent.attachments[row.rowname].pubblicazioneUrp = $scope.pubblicazioneUrp;
                        $scope.$parent.attachments[row.rowname].pubblicazioneTrasparenza = $scope.pubblicazioneTrasparenza;
                        $scope.$parent.attachments[row.rowname].protocollo = $scope.protocollo;

                    }
                }

                $scope.

                $scope.open = function() {
                    $scope.popupOpen = true;
                };
            }
        }
    }
})();