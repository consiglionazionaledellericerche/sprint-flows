package it.cnr.si.flows.ng.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.web.multipart.MultipartFile;

public class MimetypeUtils {


    public static String getMimetype(MultipartFile file) {

        try {
            // Firefox non gioca pulito coi mimetypes e sbaglia i pdf
            ByteArrayInputStream bais = getBais(file);
            return getMimetype(bais);
        } catch (IOException e) {
            return MimeTypes.OCTET_STREAM;
        }
    }

    public static String getMimetype(ByteArrayInputStream bais) {
        try {
            // Firefox non gioca pulito coi mimetypes e sbaglia i pdf
            String mimeType = new Tika().detect(bais);
            return mimeType;
        } catch (IOException e) {
            return MimeTypes.OCTET_STREAM;
        }
    }

    public static ByteArrayInputStream getBais(MultipartFile file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(file.getInputStream(), baos);
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

    public static String getMimetype(byte[] bytes) {
        return new Tika().detect(bytes);
    }

}
