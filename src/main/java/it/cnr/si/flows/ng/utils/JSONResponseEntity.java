package it.cnr.si.flows.ng.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

public class JSONResponseEntity {
	private static final Logger LOGGER = LoggerFactory.getLogger(JSONResponseEntity.class);

	private JSONResponseEntity(){
	}

	public static ResponseEntity<String> badRequest(String message) {
		return getResponse(HttpStatus.BAD_REQUEST, message);
	}

	public static ResponseEntity<String> forbidden(String message) {
		return getResponse(HttpStatus.FORBIDDEN, message);
	}

	public static ResponseEntity<String> notFound(String message) {
		return getResponse(HttpStatus.NOT_FOUND, message);
	}
	
	public static ResponseEntity<String> notAcceptable(String message) {
		return getResponse(HttpStatus.NOT_ACCEPTABLE, message);
	}

	public static ResponseEntity<String> preconditionFailed(String message) {
		return getResponse(HttpStatus.PRECONDITION_FAILED, message);
	}

	public static <T> ResponseEntity<T> ok(T body) {
		BodyBuilder builder = ResponseEntity.ok();
		return builder.body(body);
	}

	public static ResponseEntity<Void> ok() {
		BodyBuilder builder = ResponseEntity.ok();
		return builder.build();
	}
	
	public static ResponseEntity<String> getResponse(HttpStatus status, String message) {
		JSONObject entity = new JSONObject();
		try {
			entity.put("isFromApplication", true);
			entity.put("message", message);
		} catch (JSONException e) {
			LOGGER.error("Errore in fase di costruzione JSON di risposta.", e);
			ResponseEntity.badRequest().body("{\"isFromApplication\":\"true\",\"error\":\"Errore in fase di costruzione JSON di risposta.\"}");
		}
		BodyBuilder builder = ResponseEntity.status(status);
		return builder.body(entity.toString());
	}

}
