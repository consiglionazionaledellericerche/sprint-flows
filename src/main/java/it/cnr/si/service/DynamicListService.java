package it.cnr.si.service;


import it.cnr.si.domain.Dynamiclist;
import it.cnr.si.flows.ng.exception.SiglaFailedException;
import it.cnr.si.flows.ng.utils.Enum;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


@Service
@Transactional
public class DynamicListService {

    private static final String URL_STRING = "http://sigla-main-rest-missioni.test.si.cnr.it/SIGLA/";
    private static final String QUERY_STRING = "{\"activePage\" : 0, \"maxItemsPerPage\" : 1000000,\"context\" : {\"esercizio\" : 2017,\"cd_unita_organizzativa\" : \"999.000\",\"cd_cds\" : \"999\",\"cd_cdr\" : \"999.000.000\"},\"clauses\"}";
    private final Logger log = LoggerFactory.getLogger(DynamicListService.class);
    @Value("${cnr.sigla.usr}")
    private String usr;
    @Value("${cnr.sigla.psw}")
    private String psw;

    @Cacheable(value = "siglaDynamicList")
    public Dynamiclist getSiglaDynamicList(String name) {
        String jsonResponse = null;
        Dynamiclist dynamiclist = new Dynamiclist();
        try {
            URL url = new URL(makeUrl(name));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            //setta il parametro per avere una stringa nell'inputStream di risposta
            conn.setDoOutput(true);

            loginSigla(conn);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8.name());

            osw.write(makeQuery(name));
            osw.flush();
            osw.close();
            os.close();

            if (conn.getResponseCode() != 200)
                throw new SiglaFailedException("Failed : HTTP error code : " + conn.getResponseCode());
            else {
                jsonResponse = IOUtils.toString(conn.getInputStream(), StandardCharsets.ISO_8859_1.name());
                log.info("Recuperata da SIGLA la dynamic list %s:\n %s", name, jsonResponse);
                conn.disconnect();

                dynamiclist.setId((long) 1);
                dynamiclist.setName(name);
                dynamiclist.setListjson(jsonResponse);
            }
        } catch (IOException e) {
            log.error("Errore nel recupero della dynamic list da SIGLA: ", e);
        }
        return dynamiclist;
    }


    private void loginSigla(HttpURLConnection conn) {
        String login = usr + ":" + psw;
        String base64login = new String(Base64.encodeBase64(login.getBytes()));
        conn.setRequestProperty("Authorization", "Basic " + base64login);
    }

    private String makeQuery(String name) {
        String ret = "";
        switch (Enum.SiglaList.valueOf(name)) {
            case TIPOLOGIA_ACQUISIZIONE:
                ret = QUERY_STRING.replace("\"clauses\"", "\"clauses\" : [{\"condition\" : \"AND\",\"fieldName\" : \"fl_cancellato\",\"operator\" : \"=\",\"fieldValue\" : false}, {\"condition\" : \"AND\",\"fieldName\" : \"codice_anac\",\"operator\" : \"isnotnull\"}    ]");
        }

        return ret;
    }

    private String makeUrl(String name) {
        String ret = "";
        switch (Enum.SiglaList.valueOf(name)) {
            case TIPOLOGIA_ACQUISIZIONE:
                ret = URL_STRING + "ConsProcedureAmministrativeAction.json";
        }
        return ret;
    }

}
