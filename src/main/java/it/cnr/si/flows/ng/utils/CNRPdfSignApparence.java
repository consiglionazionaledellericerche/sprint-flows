package it.cnr.si.flows.ng.utils;

import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CNRPdfSignApparence extends PdfSignApparence {
    public void setImagepath(String imagepath) throws IOException {
        setImageBin(Files.readAllBytes(Paths.get(imagepath)));
    }
}
