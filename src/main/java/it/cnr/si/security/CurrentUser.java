package it.cnr.si.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import it.cnr.si.domain.User;

/**
 * XXX: non utilizzata.
 * Potrebbe essere utilizzata per arricchire lo User (spring) con informazioni aggiuntive,
 * in questo caso andrebbe utilizzata come UserDetails nel FlowUserDetailsService.
 * 
 * @author cristian
 *
 */
public class CurrentUser extends org.springframework.security.core.userdetails.User {

  private static final long serialVersionUID = -1316084495781920157L;
  
  private User user;

    public CurrentUser(User account) {
        super(account.getLogin(),
        	account.getPassword(),
        	account.getAuthorities().stream()
            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
            .collect(Collectors.toList())
        );
        this.user = account;
    }

    public CurrentUser(User account, Set<GrantedAuthority> additionalAuthorities) {
      super(account.getLogin(),
          account.getPassword(),
          account.getAuthorities().stream()
          .map(authority -> new SimpleGrantedAuthority(authority.getName()))
          .collect(Collectors.toList())
      );
      getAuthorities().addAll(additionalAuthorities);
      this.user = account;
    }
    
    public User getUser() {
        return user;
    }

}