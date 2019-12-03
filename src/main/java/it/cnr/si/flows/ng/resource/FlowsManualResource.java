package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.spring.storage.StorageObject;
import it.cnr.si.spring.storage.StorageService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("api/manual")
public class FlowsManualResource {

    private static final String DIR_MANUALI = "/Comunicazioni al CNR/flows/Manuali/";
    private static final String TITLE = "cm:title";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsManualResource.class);

    @Autowired(required = false)
    private StorageService storageService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<List<String>> getElencoManuali() {
        StorageObject dirObject = storageService.getObjectByPath(DIR_MANUALI, true);
        List<StorageObject> manuali = storageService.getChildren(dirObject.getKey());

        List<String> paths = manuali.stream()
                .map(m -> (String) m.getPropertyValue(TITLE) )
                .collect(Collectors.toList());

        return ResponseEntity.ok(paths);
    }

    @RequestMapping(value = "/{manuale}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<byte[]> getManuale(@PathVariable("manuale") String manuale) throws IOException {
        StorageObject manObject = storageService.getObjectByPath(DIR_MANUALI + manuale, false);

        InputStream stream = storageService.getInputStream(manObject.getKey());

        return ResponseEntity.ok(IOUtils.toByteArray(stream) );
    }
}
