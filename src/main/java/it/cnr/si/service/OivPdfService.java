package it.cnr.si.service;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rst.pdfbox.layout.elements.ControlElement;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.ImageElement;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.BaseFont;
import rst.pdfbox.layout.text.Position;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;


/**
 * Created by Paolo on 13/06/17.
 */
@Service
public class OivPdfService {

	public static final String TITLE = "title";
	private static final Logger LOGGER = LoggerFactory.getLogger(OivPdfService.class);
	private static final float FONT_SIZE = 10;
	private static final float TITLE_SIZE = 18;

    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private ViewRepository viewRepository;
	private Utils utils = new Utils();


	public String createPdf(String processInstanceId, ByteArrayOutputStream outputStream) throws IOException, ParseException {

		Document pdf = new Document(40, 60, 40, 60);
		Paragraph paragraphField = new Paragraph();

		LOGGER.info("createPdf - ProcessInstanceId: " + processInstanceId);
		//FlowsProcessInstanceService flowsProcessInstanceService = new FlowsProcessInstanceService();

		Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
		//Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);

		HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
		String fileName = "Preavviso di Rigetto.pdf";


		List<RestVariable> variables = processInstance.getVariables();
		ArrayList<Map> tasksSortedList = (ArrayList<Map>) map.get("history");
		Collections.reverse(tasksSortedList);  //ordino i task rispetto alla data di creazione (in senso crescente)

		//      genero il titolo del pdf (la bussineskey (es: "Acquisti Trasparenza-2017-1") + oggetto (es: "acquisto pc")
		String titolo = processInstance.getBusinessKey() + "\n";
		Optional<RestVariable> variable = variables.stream()
				.filter(a -> (a.getName().equals(oggetto.name())))
				.findFirst();
		if (variable.isPresent())
			titolo += variable.get().getValue() + "\n\n";
		else {
			// Titolo nel file pdf in caso di Workflow Definition che non ha il titolo nella variabile "oggetto"
			variable = variables.stream()
					.filter(a -> (a.getName()).equals(title.name()))
					.findFirst();

			titolo += variable.get().getValue() + "\n\n";
		}
		paragraphField.addText(titolo, TITLE_SIZE, HELVETICA_BOLD);

		//variabili da visualizzare per forza (se presenti)
		for (RestVariable var : variables) {
			String variableName = var.getName();
			if (variableName.equals(initiator.name())) {
				paragraphField.addText("Avviato da: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(startDate.name())) {
				if (var.getValue() != null)
					paragraphField.addText("Avviato il: " + formatDate(utils.parsaData((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(endDate.name())) {
				if (var.getValue() != null)
					paragraphField.addText("Terminato il: " + formatDate(utils.parsaData((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(gruppoRA.name())) {
				paragraphField.addText("Gruppo Responsabile Acquisti: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals("nomeRichiedente")) {
				fileName = "Preavviso di Rigetto " + var.getValue() + ".pdf";
				LOGGER.debug("creating pdf {} ", fileName);
			}            

		}

		//variabili "visibili" (cioè presenti nella view nel db)
		View viewToDb = viewRepository.getViewByProcessidType(processInstance.getProcessDefinitionId().split(":")[0], "detail");
		Elements metadatums = Jsoup.parse(viewToDb.getView()).getElementsByTag("metadatum");
		for (org.jsoup.nodes.Element metadatum : metadatums) {
			String label = metadatum.attr("label");
			String type = metadatum.attr("type");

			if (type.equals("table")) {
				variable = variables.stream()
						.filter(a -> (a.getName()).equals(getPropertyName(metadatum, "rows")))
						.findFirst();
				if (variable.isPresent()) {
					paragraphField.addText(label + ":\n", FONT_SIZE, HELVETICA_BOLD);
					JSONArray impegni = new JSONArray((String) variable.get().getValue());
					for (int i = 0; i < impegni.length(); i++) {
						JSONObject impegno = impegni.getJSONObject(i);

						addLine(paragraphField, "Impegno numero " + (i + 1), "", true, false);
						JSONArray keys = impegno.names();
						for (int j = 0; j < keys.length(); j++) {
							String key = keys.getString(j);
							addLine(paragraphField, key, impegno.getString(key), true, true);
						}
					}
					//Fine del markup indentato
					paragraphField.addMarkup("-!\n", FONT_SIZE, BaseFont.Helvetica);
				}
			} else {
				variable = variables.stream()
						.filter(a -> (a.getName()).equals(getPropertyName(metadatum, "value")))
						.findFirst();
				if (variable.isPresent()) {
					variables.remove(variable.get());
					paragraphField.addText(label + ": " + variable.get().getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
				}
			}
		}


		pdf.add(paragraphField);
		pdf.save(outputStream);

		return fileName;
	}

	private String getPropertyName(Element metadatum, String attr) {
		String propertyName = "";
		propertyName = metadatum.attr(attr);
		propertyName = propertyName.substring(propertyName.lastIndexOf('.') + 1).replaceAll("}", "");
		return propertyName;
	}


	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}

	private void addLine(Paragraph paragraphField, String fieldName, String fieldValue, boolean elenco, boolean subField) throws IOException {
		String text = "*" + fieldName + ":* " + fieldValue;
		if (elenco) {
			if (subField)
				paragraphField.addMarkup(" -+" + text + "\n", FONT_SIZE, BaseFont.Helvetica);
			else
				paragraphField.addMarkup("-+" + text + "\n", FONT_SIZE, BaseFont.Helvetica);
		} else
			paragraphField.addText(text + "\n", FONT_SIZE, HELVETICA_BOLD);
	}

}