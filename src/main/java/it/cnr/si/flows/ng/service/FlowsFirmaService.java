package it.cnr.si.flows.ng.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceClient;
import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceException;
import it.cnr.si.firmadigitale.firma.arss.stub.ArubaSignService;
import it.cnr.si.firmadigitale.firma.arss.stub.ArubaSignServiceService;
import it.cnr.si.firmadigitale.firma.arss.stub.Auth;
import it.cnr.si.firmadigitale.firma.arss.stub.PdfProfile;
import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.firmadigitale.firma.arss.stub.SignRequestV2;
import it.cnr.si.firmadigitale.firma.arss.stub.SignReturnV2;
import it.cnr.si.firmadigitale.firma.arss.stub.SignReturnV2Multiple;
import it.cnr.si.firmadigitale.firma.arss.stub.TypeOfTransportNotImplemented_Exception;
import it.cnr.si.firmadigitale.firma.arss.stub.TypeTransport;


/**
 * 
 * Firma Service
 * 
 * Utilizza codice da {@code ArubaSignServiceClient} in firmadigitale-1.11.jar
 * 
 * Il codice e' duplicato ed esteso.
 * 
 * @author mtrycz
 *
 */
@Profile("cnr")
@Service
public class FlowsFirmaService {

    private static final Logger LOGGER = Logger
            .getLogger(FlowsFirmaService.class);
    private static final String STATUS_OK = "OK";
    private static final String CERT_ID = "arubaRemoteSignService.certId";
    private static final String URL = "arubaRemoteSignService.url";
    private static final String TYPE_OTP_AUTH = "arubaRemoteSignService.typeOtpAuth";

    private Properties props;

    public static class FileAllaFirma {
        public String nome;
        public boolean opzionale;
        public boolean array;

        public FileAllaFirma(String nome, boolean opzionale, boolean array) {
            this.nome = nome;
            this.opzionale = opzionale;
            this.array = array;
        }
        public FileAllaFirma(String nome, boolean opzionale) {
            this.nome = nome;
            this.opzionale = opzionale;
            this.array = false;
        }
        public FileAllaFirma(String nome) {
            this.nome = nome;
            this.opzionale = false;
            this.array = false;
        }
    }
    
    public static final Map<String, List<FileAllaFirma>> NOME_FILE_FIRMA;
    static {
        NOME_FILE_FIRMA = new HashMap<>();
        NOME_FILE_FIRMA.put("acquisti#firma-decisione", Lists.newArrayList(new FileAllaFirma("decisioneContrattare")));
        NOME_FILE_FIRMA.put("acquisti#firma-provvedimento-aggiudicazione", Lists.newArrayList(new FileAllaFirma("provvedimentoAggiudicazione")));
        NOME_FILE_FIRMA.put("acquisti#firma-revoca", Lists.newArrayList(new FileAllaFirma("ProvvedimentoDiRevoca")));
        NOME_FILE_FIRMA.put("acquisti#firma-contratto", Lists.newArrayList(new FileAllaFirma("contratto")));
        NOME_FILE_FIRMA.put("accordi-internazionali-domande#firma-verbale", Lists.newArrayList(new FileAllaFirma("verbale")));
        NOME_FILE_FIRMA.put("covid19#firma", Lists.newArrayList(new FileAllaFirma("monitoraggioAttivitaCovid19")));
        NOME_FILE_FIRMA.put("short-term-mobility-bando-dipartimento#firma-graduatoria", Lists.newArrayList(new FileAllaFirma("graduatoria")));
        NOME_FILE_FIRMA.put("missioni#firma-uo", Lists.newArrayList(new FileAllaFirma("missioni"), new FileAllaFirma("anticipoMissione", true)));
        NOME_FILE_FIRMA.put("missioni#firma-spesa", Lists.newArrayList(new FileAllaFirma("missioni"), new FileAllaFirma("anticipoMissione", true)));
        NOME_FILE_FIRMA.put("firma-documenti#firma", Lists.newArrayList(new FileAllaFirma("documentoDaFirmare")));
    }

    public static final Map<String, String> ERRORI_ARUBA = new HashMap<String, String>() {{
        put("0001", "Formato file errato");
        put("0003", "Credenziali errate");
        put("0004", "PIN errato");
    }};
    
    @Value("${cnr.firma.signcertid}")
    private String RemoteSignServiceCertId;
    @Value("${cnr.firma.typeotpauth}")
    private String RemoteSignServiceTypeOtpAuth;
    @Value("${cnr.firma.url}")
    private String RemoteSignServiceUrl;
    @Value("${cnr.firma.pdfprofile}")
    private String RemotePdfprofile;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.setProperty("arubaRemoteSignService.url", RemoteSignServiceUrl);
        props.setProperty("arubaRemoteSignService.typeOtpAuth", RemoteSignServiceTypeOtpAuth);
        props.setProperty("arubaRemoteSignService.certId", RemoteSignServiceCertId);
        props.setProperty("arubaRemoteSignService.pdfprofile", RemotePdfprofile);

        this.props = props;
    }

	public byte[] firma(String username, String password, String otp, byte[] bytes, PdfSignApparence apparence) throws ArubaSignServiceException {
        
        // TODO verificare se poter usare un singolo client, e non ricrearlo ogni volta
        ArubaSignServiceClient client = new ArubaSignServiceClient();

        client.setProps(props);

        Auth identity = getIdentity(username, password, otp);

        byte[] signed = pdfsignatureV2(identity, bytes, apparence);
//        byte[] out = client.verify(signed);

        return signed;
    }

	
	public List<SignReturnV2> firmaMultipla(String username, String password, String otp, List<byte[]> files) throws ArubaSignServiceException {
		return firmaMultipla(username, password, otp, files, null);
	}

	public List<SignReturnV2> firmaMultipla(String username, String password, String otp, List<byte[]> files, PdfSignApparence pdfSignApparence) throws ArubaSignServiceException {

	    LOGGER.info("Richiesta di firma multipla per "+  files.size() +" files");
        ArubaSignService service = getServicePort();
        Auth identity = getIdentity(username, password, otp);

        try {
            List<SignRequestV2> requests = files.stream()
                    .map(b -> getRequest(identity, b))
                    .collect(Collectors.toList());

            SignReturnV2Multiple signReturnV2Multiple =
					service.pdfsignatureV2Multiple(identity, requests, pdfSignApparence, PdfProfile.fromValue(RemotePdfprofile), null);

            if (signReturnV2Multiple.getStatus().equals("OK")) {
                LOGGER.info("Firma multipla per "+  files.size() +" files completata");
                return signReturnV2Multiple.getReturnSigns();
            } else
                throw new ArubaSignServiceException(signReturnV2Multiple.getReturnCode());


        } catch (TypeOfTransportNotImplemented_Exception e) {
            throw new ArubaSignServiceException("error while invoking pdfsignatureV2", e);
        }
    }
    
    /**
     *  Questo e' il metodo personalizzato da firmadigitale-1.11.jar 
     */
    private byte[] pdfsignatureV2(Auth identity, byte[] bytes,
            PdfSignApparence apparence) throws ArubaSignServiceException {
        
        ArubaSignService service = getServicePort();
        LOGGER.debug("version " + service.getVersion());

        try {
            SignRequestV2 req = getRequest(identity, bytes);

            SignReturnV2 response = service.pdfsignatureV2(req, apparence,
                    PdfProfile.fromValue(RemotePdfprofile), null, null);

            LOGGER.debug(response.getReturnCode() +" "+ response.getStatus() +" "+ response.getDescription());

            if (response.getStatus().equals(STATUS_OK)) {
                return response.getBinaryoutput();
            } else {
                throw new ArubaSignServiceException("Server side error code "
                        + response.getReturnCode() + ", "
                        + response.getStatus() + ", "
                        + response.getDescription());
            }

        } catch (TypeOfTransportNotImplemented_Exception e) {
            throw new ArubaSignServiceException(
                    "error while invoking pdfsignatureV2", e);
        }
    }
    
    /**
     * Questo metodo di utilita' e' ricopiato dalla libreria perche' e' privato
     */
    private ArubaSignService getServicePort() throws ArubaSignServiceException {
        URL url;
        try {
            url = new URL(props.getProperty(URL));
            LOGGER.debug(url);
        } catch (MalformedURLException e) {
            throw new ArubaSignServiceException("URL: " + URL, e);
        }
        QName qname = new QName("http://arubasignservice.arubapec.it/",
                "ArubaSignServiceService");
        return new ArubaSignServiceService(url, qname)
                .getArubaSignServicePort();
    }
    
    /**
     * Questo metodo di utilita' e' ricopiato dalla libreria perche' e' privato
     */
    private SignRequestV2 getRequest(Auth identity, byte[] bytes) {
        SignRequestV2 request = new SignRequestV2();
        request.setIdentity(identity);
        request.setCertID(props.getProperty(CERT_ID));
        request.setTransport(TypeTransport.BYNARYNET);
        request.setBinaryinput(bytes);

        return request;
    }
    
    /**
     * Questo metodo di utilita' e' ricopiato dalla libreria perche' e' privato
     */
    private Auth getIdentity(String username, String password, String otp) {
        
        Auth identity = new Auth();
        identity.setUser(username);
        identity.setUserPWD(password);
        identity.setOtpPwd(otp);
        identity.setTypeOtpAuth(props.getProperty(TYPE_OTP_AUTH));
        return identity;
    }

}
