package it.cnr.si.flows.ng.service;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * TokenRequestInterceptor. Aggiunge l'access token del token jwt come header di ogni richiesta
 * fatta.
 *
 * @author daniele
 * @since 11/06/18
 */
public class TokenRequestInterceptor implements RequestInterceptor {

  private final String accessToken;

  public TokenRequestInterceptor(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public void apply(RequestTemplate requestTemplate) {
    requestTemplate.header("Authorization", "Bearer " + accessToken);
  }
}
