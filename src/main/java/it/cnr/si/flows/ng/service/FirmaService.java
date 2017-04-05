package it.cnr.si.flows.ng.service;

import java.util.Properties;

import org.springframework.stereotype.Service;

import it.cnr.jada.firma.arss.ArubaSignServiceClient;
import it.cnr.jada.firma.arss.ArubaSignServiceException;

@Service
public class FirmaService {

    private static final String USERNAME = "utentefr";
    private static final String PASSWORD = "utentefr123";
    private static final String OTP = "6629961578";
    private static final String RemoteSignServiceCertId="AS0";
    private static final String RemoteSignServiceTypeOtpAuth="firma";
    private static final String RemoteSignServiceUrl="http://arss.demo.si.cnr.it:8980/ArubaSignService/ArubaSignService?wsdl";

    public byte[] provaFirma() throws ArubaSignServiceException {

        Properties props = new Properties();
        ArubaSignServiceClient client = new ArubaSignServiceClient();

        props.setProperty("arubaRemoteSignService.url", RemoteSignServiceUrl);
        props.setProperty("arubaRemoteSignService.typeOtpAuth", RemoteSignServiceTypeOtpAuth);
        props.setProperty("arubaRemoteSignService.certId", RemoteSignServiceCertId);
        client.setProps(props);

        //byte[] bytes = getBytes(sip.toString());
        byte[] bytes = (new String("TESTO DA FIRMARE DI PROVA")).getBytes();
        byte[] content = client.pkcs7SignV2(USERNAME, PASSWORD, OTP, bytes);
        byte[] out = client.verify(content);

        return out;

    }

    public byte[] firma(String username, String password, String otp, byte[] bytes) throws ArubaSignServiceException {
        Properties props = new Properties();
        ArubaSignServiceClient client = new ArubaSignServiceClient();
        props.setProperty("arubaRemoteSignService.url", RemoteSignServiceUrl);
        props.setProperty("arubaRemoteSignService.typeOtpAuth", RemoteSignServiceTypeOtpAuth);
        props.setProperty("arubaRemoteSignService.certId", RemoteSignServiceCertId);
        client.setProps(props);

        byte[] signed = client.pkcs7SignV2(username, password, otp, bytes);
        byte[] out = client.verify(signed);

        return out;

    }
}
