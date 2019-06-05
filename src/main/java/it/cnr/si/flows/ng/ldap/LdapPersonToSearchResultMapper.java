package it.cnr.si.flows.ng.ldap;

import it.cnr.si.flows.ng.utils.Utils;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

public class LdapPersonToSearchResultMapper implements AttributesMapper<Utils.SearchResult> {

    public Utils.SearchResult mapFromAttributes(Attributes attrs) throws NamingException {
        return new Utils.SearchResult(attrs.get("uid").get().toString(),
                attrs.get("cnrnome").get() + " " + attrs.get("cnrcognome").get() + " " +
                        "(" + attrs.get("uid").get().toString() + ")");
    }

}
