package it.cnr.si.flows.ng.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.cnr.jada.firma.arss.ArubaSignServiceClient;
import it.cnr.jada.firma.arss.ArubaSignServiceException;

@Service
public class FlowsFirmaService {

    @Value("${cnr.firma.signcertid}")
    private String RemoteSignServiceCertId;
    @Value("${cnr.firma.typeotpauth}")
    private String RemoteSignServiceTypeOtpAuth;
    @Value("${cnr.firma.url}")
    private String RemoteSignServiceUrl;

    public byte[] firma(String username, String password, String otp, byte[] bytes) throws ArubaSignServiceException {
        Properties props = new Properties();
        ArubaSignServiceClient client = new ArubaSignServiceClient();
        props.setProperty("arubaRemoteSignService.url", RemoteSignServiceUrl);
        props.setProperty("arubaRemoteSignService.typeOtpAuth", RemoteSignServiceTypeOtpAuth);
        props.setProperty("arubaRemoteSignService.certId", RemoteSignServiceCertId);
        client.setProps(props);

        // TODO Convenire su un formato adeguato per la firma grafica, se la si vuole
//        PdfSignApparence apparence = new PdfSignApparence();
//        apparence.setLeftx(100);
//        apparence.setLefty(100);
//        apparence.setRightx(300);
//        apparence.setRighty(200);
//        apparence.setPage(1);
//        apparence.setLocation("Rome");
//
//        apparence.setTesto("Firmato digitalmente da "+ username +" in data "+ new Date());

        byte[] signed = client.pdfsignatureV2(username, password, otp, bytes);
//        byte[] out = client.verify(signed);

        return signed;

    }
}
