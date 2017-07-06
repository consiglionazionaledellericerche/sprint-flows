package it.cnr.si.service;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessDiagramService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricIdentityLinkResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jsoup.Jsoup;
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

import javax.inject.Inject;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;


/**
 * Created by Paolo on 13/06/17.
 */
@Service
public class SummaryPdfService {

    public static final String TITLE = "title";
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryPdfService.class);
    private static final float FONT_SIZE = 10;
    private static final float TITLE_SIZE = 18;

    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private FlowsProcessDiagramService flowsProcessDiagramService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private ViewRepository viewRepository;
    private Utils utils = new Utils();


    public String createPdf(String processInstanceId, ByteArrayOutputStream outputStream) throws IOException, ParseException {

        Document pdf = new Document(40, 60, 40, 60);
        Paragraph paragraphField = new Paragraph();
        Paragraph paragraphDiagram = new Paragraph();
        Paragraph paragraphDocs = new Paragraph();
        Paragraph paragraphHistory = new Paragraph();

        Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);

        HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
        String fileName = "Summary_" + processInstance.getBusinessKey() + ".pdf";
        LOGGER.debug("creating pdf {} ", fileName);

        List<RestVariable> variables = processInstance.getVariables();
        ArrayList<Map> tasksSortedList = (ArrayList<Map>) map.get("history");
        Collections.reverse(tasksSortedList);  //ordino i task rispetto alla data di creazione (in senso crescente)

//      genero il titolo del pdf (la bussineskey (es: "Acquisti Trasparenza-2017-1") + titoloIstanzaFlusso (es: "acquisto pc")
        String titolo = processInstance.getBusinessKey() + "\n";
        Optional<RestVariable> variable = variables.stream()
                .filter(a -> (a.getName()).equals("titoloIstanzaFlusso"))
                .findFirst();
        if (variable.isPresent())
            titolo += variable.get().getValue() + "\n\n";
        else {
            // Titolo nel file pdf in caso di Workflow Definition che non ha il titolo nella variabile "titoloIstanzaFlusso"
            variable = variables.stream()
                    .filter(a -> (a.getName()).equals("title"))
                    .findFirst();

            titolo += variable.get().getValue() + "\n\n";
        }
        paragraphField.addText(titolo, TITLE_SIZE, HELVETICA_BOLD);

        //variabili da visualizzare per forza (se presenti)
        for (RestVariable var : variables) {
            String variableName = var.getName();
            if (variableName.equals("initiator")) {
                paragraphField.addText("Avviato da: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);

            } else if (variableName.equals("startDate")) {
                if (var.getValue() != null)
                    paragraphField.addText("Avviato il: " + formatDate(utils.format.parse((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);

            } else if (variableName.equals("endDate")) {
                if (var.getValue() != null)
                    paragraphField.addText("Terminato il: " + formatDate(utils.format.parse((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);

            } else if (variableName.equals("gruppoRA")) {
                paragraphField.addText("Gruppo Responsabile Acquisti: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);

            } else if (variableName.equals("impegniVeri")) {
                paragraphField.addText("Lista Impegni: \n", FONT_SIZE, HELVETICA_BOLD);
                JSONArray impegni = new JSONArray((String) var.getValue());
                for (int i = 0; i < impegni.length(); i++) {
                    JSONObject impegno = impegni.getJSONObject(i);

                    addLine(paragraphField, "Impegno numero " + (i + 1), "", true, false);
                    JSONArray keys = impegno.names();
                    for (int j = 0; j < keys.length(); j++) {
                        String key = keys.getString(j);
                        addLine(paragraphField, key, impegno.getString(key), true, true);
                    }
                }
                //  Fine del markup indentato
                paragraphField.addMarkup("-!\n", FONT_SIZE, BaseFont.Helvetica);
            }
        }

        //variabili "visibili" (cioè presenti nella view nel db
        View viewToDb = viewRepository.getViewByProcessidType(processInstance.getProcessDefinitionId().split(":")[0], "detail");
        Elements metadatums = Jsoup.parse(viewToDb.getView()).getElementsByTag("metadatum");
        for (org.jsoup.nodes.Element metadatum : metadatums) {
            String[] appo = metadatum.val().replace("{", "").replace("}", "").replace("$", "").split("\\.");
            String property = appo[appo.length - 1];

            variable = variables.stream()
                    .filter(a -> (a.getName()).equals(property))
                    .findFirst();

            if (variable.isPresent()) {
                variables.remove(variable.get());
                String label = metadatum.getElementsByAttribute("label").get(0).attr("label");
                paragraphField.addText(label + ": " + variable.get().getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
            }
        }

        //caricamento diagramma workflow
        ImageElement image = makeDiagram(processInstanceId, paragraphDiagram, new PDPage().getMediaBox().createDimension());

        //caricamento documenti allegati al flusso e cronologia
        makeDocs(paragraphDocs, processInstanceId);

        //caricamento history del workflow
        makeHistory(paragraphHistory, tasksSortedList);

        pdf.add(paragraphField);
        pdf.add(ControlElement.NEWPAGE);
        pdf.add(paragraphDiagram);
        pdf.add(image);
        pdf.add(ControlElement.NEWPAGE);
        pdf.add(paragraphDocs);
        pdf.add(ControlElement.NEWPAGE);
        pdf.add(paragraphHistory);

        pdf.save(outputStream);

        return fileName;
    }


    private void makeHistory(Paragraph paragraphHistory, ArrayList<Map> tasksSortedList) throws IOException {
        for (Map task : tasksSortedList) {
            HistoricTaskInstanceResponse historyTask = (HistoricTaskInstanceResponse) task.get("historyTask");
            ArrayList<HistoricIdentityLinkResponse> historyIdentityLinks = (ArrayList<HistoricIdentityLinkResponse>) task.get("historyIdentityLink");

            addLine(paragraphHistory, "Titolo task", historyTask.getName(), true, false);
            addLine(paragraphHistory, "Avviato il ", formatDate(historyTask.getStartTime()), true, true);
            if (historyTask.getEndTime() != null)
                addLine(paragraphHistory, "Terminato il ", formatDate(historyTask.getEndTime()), true, true);

            for (HistoricIdentityLinkResponse il : historyIdentityLinks) {
                addLine(paragraphHistory, il.getType(), (il.getUserId() == null ? il.getGroupId() : il.getUserId()), true, true);
            }
            paragraphHistory.addText("\n", FONT_SIZE, HELVETICA_BOLD);
        }
    }


    private String formatDate(Date date) {
        return date != null ? utils.formatoVisualizzazione.format(date) : "";
    }


    private ImageElement makeDiagram(String processInstanceId, Paragraph paragraphDiagram, Dimension dimension) throws IOException {
        ImageElement image = null;
        intestazione(paragraphDiagram, "Diagramma del flusso:");
        int margineSx = 50;

        InputStream diagram = flowsProcessDiagramService.getDiagramForProcessInstance(processInstanceId, null);

        image = new ImageElement(diagram);
        Dimension scaledDim = getScaledDimension(new Dimension((int) image.getWidth(), (int) image.getHeight()),
                                                 dimension, margineSx);
        image.setHeight((float) scaledDim.getHeight());
        image.setWidth((float) scaledDim.getWidth());
        image.setAbsolutePosition(new Position(20, 700));
        return image;
    }


    private void makeDocs(Paragraph paragraphDocs, String processInstancesId) throws IOException {

        intestazione(paragraphDocs, "Documenti del flusso:");
        Map<String, FlowsAttachment> docs = flowsAttachmentService.getAttachementsForProcessInstance(processInstancesId);

        for (Map.Entry<String, FlowsAttachment> entry : docs.entrySet()) {
            FlowsAttachment doc = entry.getValue();
            addLine(paragraphDocs, entry.getKey(), doc.getName(), true, false);

            addLine(paragraphDocs, "Nome del file", doc.getFilename(), true, true);
            addLine(paragraphDocs, "Caricato il", formatDate(doc.getTime()), true, true);
            addLine(paragraphDocs, "Dall'utente", doc.getUsername(), true, true);
            addLine(paragraphDocs, "Nel task", doc.getTaskName(), true, true);
            addLine(paragraphDocs, "Mime-Type", doc.getMimetype(), true, true);
            //Tolgo le parentesi quadre (ad es.: [Firmato, Protocollato, Pubblicato]
            String stati = doc.getStati().toString().replace("[", "").replace("]", "");
            if (!stati.isEmpty())
                addLine(paragraphDocs, "Stato Documento", stati, true, true);
            //doppio a capo dopo ogni documento
            paragraphDocs.addText("\n\n", FONT_SIZE, HELVETICA_BOLD);
        }
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


    private void intestazione(Paragraph contentStream, String titolo) throws IOException {
        contentStream.addText(titolo + "\n\n", TITLE_SIZE, HELVETICA_BOLD);
    }


    private Dimension getScaledDimension(Dimension imgSize, Dimension boundary, int marginRigth) {
        int originalWidth = imgSize.width;
        int originalHeight = imgSize.height;
        int boundWidth = boundary.width;
        int boundHeight = boundary.height;
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // controllo se abbiamo bisogno di "scalare" la larghezza
        if (originalWidth > boundWidth) {
            //adatto la larghezza alla pagina ed ai margini
            newWidth = boundWidth - marginRigth;
            //adatto l'altezza per mantenere le proporzioni con la nuova larghezza "scalata"
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // verifico se c'è bisogno di scalare anche con la nuova altezza
        if (newHeight > boundHeight) {
            //"scalo" l'altezza
            newHeight = boundHeight;
            //adatto la larghezza per adattarla all'altezza "scalata"
            newWidth = ((newHeight * originalWidth) / originalHeight) - marginRigth;
        }
        return new Dimension(newWidth, newHeight);
    }
}