package it.cnr.si.flows.ng.utils.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.si.flows.ng.exception.AwesomeException;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class ResultProxy implements Serializable {
    private String type;
    private HttpStatus status;
    private String body;
    private CommonJsonRest<RestServiceBean> commonJsonResponse;
    
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public HttpStatus getStatus() {
		return status;
	}
	public void setStatus(HttpStatus status) {
		this.status = status;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public CommonJsonRest<RestServiceBean> getCommonJsonResponse() {
		return commonJsonResponse;
	}
	public void setCommonJsonResponse(
			CommonJsonRest<RestServiceBean> commonJsonResponse) {
		this.commonJsonResponse = commonJsonResponse;
	}
	@Override
	public String toString() {
		String jsonBody = null;
    	try {
    		ObjectMapper mapper = new ObjectMapper();
    		jsonBody = mapper.writeValueAsString(body);
    	} catch (Exception ex) {
    		throw new AwesomeException(1, "Errore nella manipolazione del file JSON per la responde della REST");
    	}
		return "CallCache [status=" + status + ", body=" + jsonBody.toString()
				+ ", type=" + type + ", commonJsonResponse=" + commonJsonResponse;
	}
}

