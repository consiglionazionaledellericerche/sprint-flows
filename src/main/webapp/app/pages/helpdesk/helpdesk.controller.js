(function() {
  "use strict";

  angular
    .module("sprintApp")
    .controller("HelpdeskController", HelpdeskController);

  HelpdeskController.$inject = [
    "$scope",
    "dataService",
    "$log",
    "$http",
    "URL_REST",
    "APP_FOR_REST",
    "OIL_REST",
    "$uibModal",
    "$state"
  ];

  function HelpdeskController(
    $scope,
    dataService,
    $log,
    $http,
    URL_REST,
    APP_FOR_REST,
    OIL_REST,
    $uibModal,
    $state
  ) {
    var dataTree = [],
      hdDataModel = {};

    function appo(jsonOil) {
      jsonOil.forEach(element => {
        var el = {};
        el.text = element.descrizione;
        if (element.idPadre === 56) {
          el.parent = "#";
        } else {
          el.parent = element.idPadre;
        }
        el.icon = "glyphicon glyphicon-folder-close";
        el.id = element.id;
        dataTree.push(el);

        if (element.sottocategorie) {
          appo(element.sottocategorie);
        }
      });
    }

    var vm = this;

    vm.categorie = [];
    $scope.restCategorie = function() {
      var urlRestProxy = URL_REST.STANDARD,
        app = APP_FOR_REST.OIL,
        url = OIL_REST.CATEGORIE,
        tree = {};

      $http
        .get(urlRestProxy + app + "/" + "?proxyURL=" + url)
        .success(function(result) {
          if (result) {
            var provaCategorie = result.filter(function(el) {
              return el.nome === "Scrivania Digitale";
            });

            vm.jsTree = { version: 1, core: { themes: { name: "default" } } };
            appo(provaCategorie[0].sottocategorie);

            tree.core = {};
            tree.core.data = [];
            tree.core.data = dataTree;

            $("#categorieTree")
              .jstree({
                core: {
                  data: dataTree
                },
                plugins: ["wholerow"]
              })
              .on("changed.jstree", function(e, data) {
                if (data.instance.is_leaf(data.node)) {
                  vm.categoria = data.node;
                } else {
                  data.instance.deselect_node(data.node, false)
                  vm.categoria = undefined;
                }
              })
              .on("open_node.jstree", function(e, data) {
                data.instance.set_icon(
                  data.node,
                  "glyphicon glyphicon-folder-open"
                );
              })
              .on("close_node.jstree", function(e, data) {
                data.instance.set_icon(
                  data.node,
                  "glyphicon glyphicon-folder-close"
                );
              });
          }
        });
    };

    var initMapHelpDesk = function() {
      $scope.restCategorie();
      delete $scope.helpdeskModel;
      $("#files")
        .children()
        .remove();
      $('button[name="sendMail"]').unbind("click");
      $('button[name="sendMail"]').click(function() {
        hdDataModel.titolo = $scope.helpdeskModel.titolo;
        hdDataModel.descrizione = $scope.helpdeskModel.descrizione;
        hdDataModel.categoria = vm.categoria.id;
        hdDataModel.categoriaDescrizione = vm.categoria.text;

        dataService.helpdesk.sendWithoutAttachment(hdDataModel).then(
          function(response) {
            if (response.data.segnalazioneId) {
              $uibModal.open({
                template: `<div class="modal-header">
									<h4 class="modal-title">Segnalazione inviata correttamente</h4>
								  </div>
								  <div class="modal-body">
									<button class="btn btn-primary" type="button" ng-click="fatto()"><span class="glyphicon glyphicon-remove"></span> Chiudi</button>
								  </div>`,
				scope: $scope
              });
            }
          },
          function(error) {
            $log.error(error);
            $uibModal.open({
              template: `<div class="modal-header">
								<h4 class="modal-title">Segnalazione NON inviata per problemi tecnici: riprovare in seguito</h4>
							  </div>
							  <div class="modal-body">
								<button class="btn btn-primary" type="button" ng-click="$dismiss()"><span class="glyphicon glyphicon-remove"></span> Chiudi</button>
							  </div>`
            });
          }
        );
      });
    };
    initMapHelpDesk();

    // $('#fileupload').fileupload({
    // 	url: 'api/rest/helpdesk/sendWithAttachment',
    // 	dataType: 'json',
    // 	maxNumberOfFiles: 1,
    // 	progressInterval: 1000,
    // 	add: function (e, data) {
    // 		$('button[name="sendMail"]').unbind( "click" );
    // 		$('button[name="sendMail"]').click(function () {
    // 			data.formData = new FormData();
    // 			data.formData.append("titolo", $scope.helpdeskModel.titolo);
    // 			data.formData.append("descrizione", $scope.helpdeskModel.descrizione);
    // 			data.formData.append("nota", $scope.helpdeskModel.nota);
    // 			data.formData.append("idSegnalazione", $scope.idHelpdesk);
    // 			data.formData.append("categoria", $scope.helpdeskModel.categoria);
    // 			for (var k=0; k<$scope.categorie.length; k++) {
    // 				if ($scope.helpdeskModel.categoria == $scope.categorie[k].id){
    // 				data.formData.append("categoriaDescrizione", $scope.categorie[k].descrizione);
    // 				}
    // 			}
    // 			data.submit();
    // 		});
    // 		$('#files').children().remove();
    // 		$.each(data.files, function (index, file) {
    // 			$('<p/>').text(file.name).appendTo($('#files'));
    // 		});
    // 	},
    // 	progressall: function (e, data) {
    // 		var progress = parseInt(data.loaded / data.total * 100, 10);
    // 		$('#progress .progress-bar').css(
    // 			'width',
    // 			progress + '%'
    // 		);
    // 	},
    // 	fail: function(e, data) {
    // 		if (data.jqXHR.status===200){
    // 			ui.message("Segnalazione inviata.");
    // 		}else{
    // 			ui.error("Errore nell'invio della segnalazione. Riprovare successivamente.")
    // 		}
    // 		initMapHelpDesk();
    // 		$scope.$apply();
    // 	},
    // 	beforeSend: function(xhr) {
    // 		xhr.setRequestHeader("Authorization", "Bearer "+$scope.accessToken);
    // 	}
    // })
    // .prop('disabled', !$.support.fileInput)
    // .parent().addClass($.support.fileInput ? undefined : 'disabled');

    vm.sendMailButtonDisable = function() {
      // todo: la validazione avviene solo dopo aver cambiato il contenuto delle aree di testo
      return !(
        vm.categoria !== undefined &&
        vm.categoria.id !== undefined &&
        vm.categoria.children.length == 0 &&
        $scope.helpdeskModel.descrizione !== undefined &&
        $scope.helpdeskModel.titolo !== undefined
      );
    };

    $scope.trustAsHtml = function(html) {
      return $scope.trustAsHtml(html);
    };

    $scope.fatto = function() {
      $state.go('home');
    }
  }
})();
