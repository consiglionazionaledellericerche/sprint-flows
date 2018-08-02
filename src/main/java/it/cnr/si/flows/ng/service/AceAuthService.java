package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.utils.AceJwt;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * AuthService.
 *
 * @author daniele
 * @since 08/06/18
 */
@Headers({"Authorization: Basic {basic}",
    "Content-Type: application/x-www-form-urlencoded"})
public interface AceAuthService {

  String BASIC = "YWNlOnRoaXNpc3NlY3JldA==";
  String SCOPE = "webclient";
  String TOKEN_GRANT = "password";
  String REFRESH_TOKEN_GRANT = "refresh_token";

  @RequestLine("POST /authentication/auth/oauth/token")
  AceJwt getTokenFull(
          @Param("basic") String basic,
          @Param("grant_type") String grantType,
          @Param("scope") String scope,
          @Param("username") String username,
          @Param("password") String password);

  default AceJwt getToken(@Param("username") String username, @Param("password") String password) {
    return getTokenFull(BASIC, TOKEN_GRANT, SCOPE, username, password);
  }

  @RequestLine("POST /authentication/auth/oauth/token")
  AceJwt getRefreshedTokenFull(
          @Param("basic") String basic,
          @Param("grant_type") String grantType,
          @Param("scope") String scope,
          @Param("refresh_token") String refreshToken);

  default AceJwt getRefreshedToken(@Param("refresh_token") String refreshToken) {
    return getRefreshedTokenFull(BASIC, REFRESH_TOKEN_GRANT, SCOPE, refreshToken);
  }


}

