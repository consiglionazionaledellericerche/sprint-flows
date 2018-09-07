package it.cnr.si.flows.ng.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Jwt.
 *
 * @author daniele
 * @since 08/06/18
 */
@ToString
@Getter
@Setter
public class AceJwt {

  private String access_token;
  private String token_type;
  private String refresh_token;
  private int expires_in;
  private String scope;
  private List<Auth> auth;
  private User user;
  private String jti;

  @ToString
  @Getter
  @Setter
  private class Auth {

    private String role;
    private int group;
  }

  @ToString
  @Getter
  @Setter
  private class User {

    private String username;
    private String password;
    private String matricola;
    private String email;
    private boolean enabled;
    private String departmentNumber;
    private String lastName;
    private String firstName;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean accountNonExpired;
    private List<Authority> authorities;

  }

  @ToString
  @Getter
  @Setter
  private class Authority {

    private String authority;
  }

}
