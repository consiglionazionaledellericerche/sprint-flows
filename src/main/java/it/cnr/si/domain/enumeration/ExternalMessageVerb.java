package it.cnr.si.domain.enumeration;

import org.springframework.http.HttpMethod;

/**
 * The ExternalMessageVerb enumeration.
 */
public enum ExternalMessageVerb {
    POST(HttpMethod.POST),
    GET(HttpMethod.GET),
    PUT(HttpMethod.PUT),
    PATCH(HttpMethod.PATCH),
    DELETE(HttpMethod.DELETE);

    private HttpMethod method;

    public HttpMethod value() {
        return this.method;
    }

    private ExternalMessageVerb(HttpMethod value) {
        this.method = value;
    }
}
