package it.cnr.si.flows.ng.utils;

import org.springframework.core.io.ByteArrayResource;

public class FileMessageResource extends ByteArrayResource {

    /**
     * The filename to be associated with the {@link MimeMessage} in the form data.
     */
    private final String filename;

    /**
     * Constructs a new {@link FileMessageResource}.
     * @param byteArray A byte array containing data from a {@link MimeMessage}.
     * @param filename The filename to be associated with the {@link MimeMessage} in
     * 	the form data.
     */
    public FileMessageResource(final byte[] byteArray, final String filename) {
        super(byteArray);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
