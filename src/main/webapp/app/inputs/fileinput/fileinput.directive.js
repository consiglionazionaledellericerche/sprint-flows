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
                multiple: '=?',
                cnrRequired: '=?',
                metadatiPubblicazione: '@?',
                metadatiProtocollo: '@?',
                pubblicazioneUrp: '@?',
                pubblicazioneTrasparenza: '@?',
                protocollo: '@?'
            },
            link: function ($scope, element, attrs) {

                function init() {
                    $scope.min = $scope.min || 0;
                    $scope.max = $scope.max || 999;

                    $scope.data = $scope.$parent.data;

                    $scope.pubblicazioneUrp = ($scope.pubblicazioneUrp == 'true');
                    $scope.pubblicazioneTrasparenza = ($scope.pubblicazioneTrasparenza == 'true');
                    $scope.metadatiPubblicazione = ($scope.metadatiPubblicazione == 'true' || ($scope.pubblicazioneUrp || $scope.pubblicazioneTrasparenza));
                    $scope.metadatiProtocollo = ($scope.metadatiProtocollo == 'true');
                    $scope.protocollo = ($scope.protocollo == 'true');

                    $scope.scope = $scope;
                    $scope.attrs = attrs;

                    if ($scope.multiple) {
                        $scope.rows = [];
                        $scope.$parent.attachments.forEach(function (el) {
                            if (el.name.startsWith($scope.name))
                                $scope.rows.push({});
                        })
                    } else {
                        $scope.rows = [{}]
                    }


                    if ($scope.$parent.attachments === undefined)
                        $scope.$parent.attachments = [];

                }

                $scope.$watch($scope.rows, function(newValue) {

                    // copy over newValue to $scope.$parent.data

                }, true);


                init();

                $scope.findDocument = function(name) {
                    return $scope.$parent.attachments.find(function(el) {
                        return el.name === name;
                    });
                }

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
                    var name = $scope.name+$scope.rows.length;
                    delete $scope.$parent.data[name];
                    delete $scope.$parent.data[name+'_name'];
                    delete $scope.$parent.data[name+'_pubblicazioneUrp'];
                    delete $scope.$parent.data[name+'_pubblicazioneTrasparenza'];
                    delete $scope.$parent.data[name+'_protocollo'];
                    delete $scope.$parent.data[name+'_dataprotocollo'];
                    delete $scope.$parent.data[name+'_numeroprotocollo'];
                    return false;
                };

                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                }
            }
        }
    }
})();