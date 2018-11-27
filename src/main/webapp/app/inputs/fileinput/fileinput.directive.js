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
                protocollo: '=?'
            },
            link: function ($scope, element, attrs) {

                function init() {
                    $scope.min = $scope.min || 0;
                    $scope.max = $scope.max || 999;

                    $scope.attrs = attrs;

                    if ($scope.$parent.attachments === undefined)
                        $scope.$parent.attachments = {};

                    $scope.attachments = $scope.$parent.attachments;

                    // se sono in modifica, reimposto i dati
                    if ($scope.multiple) {
                        $scope.rows = [];
                        for (var el in $scope.$parent.attachments) {
                            if (el.name.startsWith($scope.name))
                                $scope.rows.push({});
                        }
                    } else {
                        $scope.rows = [{}]
                    }
                }

                init();

//                $scope.$watch('rows', function(newValue) {
//
//                    for (var i = 0; i < $scope.rows.length; i++) {
//                        var row = $scope.rows[i];
//                        var name = $scope.name + ($scope.multiple ? i : '');
//                        $scope.$parent.data[name] = row.data;
//                        $scope.$parent.data[name+'_name'] = row.name;
//                        $scope.$parent.data[name+'_pubblicazioneUrp'] = row.pubblicazioneUrp;
//                        $scope.$parent.data[name+'_pubblicazioneTrasparenza'] = row.pubblicazioneTrasparenza;
//                        $scope.$parent.data[name+'_protocollo'] = row.protocollo;
//                        $scope.$parent.data[name+'_dataprotocollo'] = row.dataprotocollo;
//                        $scope.$parent.data[name+'_numeroprotocollo'] = row.numeroprotocollo;
//                    }
//
//                });


                $scope.findDocument = function(name) {
                    var result = {};
                    for (var el in $scope.$parent.attachments) {
                        if (el.name === name)
                            result = el;
                    };
                    return result;
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
//                    var name = $scope.name+$scope.rows.length;
//                    delete $scope.$parent.data[name];
//                    delete $scope.$parent.data[name+'_name'];
//                    delete $scope.$parent.data[name+'_pubblicazioneUrp'];
//                    delete $scope.$parent.data[name+'_pubblicazioneTrasparenza'];
//                    delete $scope.$parent.data[name+'_protocollo'];
//                    delete $scope.$parent.data[name+'_dataprotocollo'];
//                    delete $scope.$parent.data[name+'_numeroprotocollo'];
//                    return false;
                };

                $scope.downloadFile = function(url, filename, mimetype) {
                    utils.downloadFile(url, filename, mimetype);
                }
            }
        }
    }
})();