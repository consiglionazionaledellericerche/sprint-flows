package it.cnr.si.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.base.MoreObjects;

/**
 * Seam like check.
 *
 * @author marco
 * @author cristian
 */
@RequiredArgsConstructor @Getter
public class PermissionCheck {

    private final Object target;
    private final Object permission;
    private boolean granted = false;

    public void grant() {
    	this.granted = true;
    }

    public void revoke() {
    	this.granted = false;
    }

    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this).omitNullValues()
    			.add("action", permission)
    			.add("target", target)
    			.addValue(granted ? "GRANTED" : "DENIED").toString();
    }
}
