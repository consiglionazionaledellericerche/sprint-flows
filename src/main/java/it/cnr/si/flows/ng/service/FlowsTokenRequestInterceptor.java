package it.cnr.si.flows.ng.service;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * FlowsTokenRequestInterceptor. Aggiunge l'access token del token jwt come header di ogni richiesta
 * fatta.
 *
 * @author daniele
 * @since 11/06/18
 */
public class FlowsTokenRequestInterceptor implements RequestInterceptor {

  private final String accessToken;

  public FlowsTokenRequestInterceptor(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public void apply(RequestTemplate requestTemplate) {
    requestTemplate.header("Authorization", "Bearer " + accessToken);
  }
}
