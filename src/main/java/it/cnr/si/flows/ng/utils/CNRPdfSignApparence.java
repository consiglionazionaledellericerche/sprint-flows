package it.cnr.si.flows.ng.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;

public class CNRPdfSignApparence extends PdfSignApparence {
	public void setImagepath(String imagepath) throws IOException {
		InputStream reasourceStream = getClass().getClassLoader().getResourceAsStream(imagepath);
		setImageBin(IOUtils.toByteArray(reasourceStream));
	}
}
