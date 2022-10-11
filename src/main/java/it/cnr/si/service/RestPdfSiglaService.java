package it.cnr.si.service;

import it.cnr.si.flows.ng.exception.SiglaFailedException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
@Profile({"cnr","iss"})
public class RestPdfSiglaService {

    private final Logger logger = LoggerFactory.getLogger(RestPdfSiglaService.class);
    @Value("${cnr.sigla.usr}")
    private String usr;
    @Value("${cnr.sigla.psw}")
    private String psw;
    @Value("${cnr.sigla.print.url}")
    private String printServerURL;

    public byte[] getSiglaPdf(String data) {
        byte[] pdfByteArray = null;

        try {
        	
            logger.info("************** data: {}", data);

            URL url = new URL(printServerURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            //setta il parametro per avere una stringa nell'inputStream di risposta
            conn.setDoOutput(true);

            loginSigla(conn);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8.name());

            osw.write(data);
            osw.flush();
            osw.close();
            os.close();

            if (conn.getResponseCode() != 200)
                throw new SiglaFailedException("Failed : HTTP error code : " + conn.getResponseCode());
            else {
                pdfByteArray = IOUtils.toByteArray(conn.getInputStream());
                logger.info("Recuperata da SIGLA la dynamic list %s:\n %s", data);
                conn.disconnect();
            }

        } catch (IOException e) {
            logger.error("Errore nel recupero del PDF da SIGLA: ", e);
        }
        return pdfByteArray;
    }

    private void loginSigla(HttpURLConnection conn) {
        String login = usr + ":" + psw;
        String base64login = new String(Base64.encodeBase64(login.getBytes()));
        conn.setRequestProperty("Authorization", "Basic " + base64login);
    }

}
