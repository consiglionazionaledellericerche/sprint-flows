(function() {
  'use strict';

  angular.module('sprintApp')
  .filter('data', function () {
    return function(d) {
        return d ? moment(d).format('DD/MM/YYYY HH:mm') : null;
    };
  })
  .filter('dueDate', function () {
    return function (dueDate) {
      var s = null;
      if (dueDate) {
        var expired = new Date(dueDate).getTime() < new Date().getTime();
        var stringDate = moment(dueDate).format('DD/MM/YYYY');
        if (expired) {
          s = '<span class="label label-danger">scaduto il ' + stringDate + '</span>';
        } else {
          s = '<span>con scadenza: ' + stringDate + '</span>';
        }
      }
      return s;
    };
  })
  .filter('priority', function () {
    return function (priority) {
      var m = {
          'priority-1': {
            label: 'media',
            cssClass: 'label label-success'
          },
          'priority-3': {
            label: 'importante',
            cssClass: 'label label-warning'
          },
          'priority-5': {
            label: 'critica',
            cssClass: 'label label-danger'
          }
      };

      var p = m['priority-' + priority];

      return p ? '<span class="label ' + p.cssClass + '">' + p.label + '</b>' : '';
    };
  })

})();