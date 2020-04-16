package it.cnr.si.flows.ng.utils;

import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.ResourceUtils;

public class CNRPdfSignApparence extends PdfSignApparence {
	public void setImagepath(String imagepath) throws IOException {
		InputStream reasourceStream = getClass().getClassLoader().getResourceAsStream(imagepath);
		IOUtils.toByteArray(reasourceStream);
	}
}
