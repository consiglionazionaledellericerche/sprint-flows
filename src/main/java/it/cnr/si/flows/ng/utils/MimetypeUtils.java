package it.cnr.si.flows.ng.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

public class MimetypeUtils {


    public static String getMimetype(MultipartFile file) throws IOException {
        // Firefox non gioca pulito coi mimetypes e sbaglia i pdf
        ByteArrayInputStream bais = getBais(file);
        return getMimetype(bais);
    }

    private static String getMimetype(ByteArrayInputStream bais) throws IOException {
        String mimeType = new Tika().detect(bais);
        return mimeType;
    }

    public static ByteArrayInputStream getBais(MultipartFile file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(file.getInputStream(), baos);
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

}
