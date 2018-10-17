package org.apache.cxf.rs.security.httpsignature;

import java.security.Provider;

@FunctionalInterface
public interface SecurityProvider {

    /**
     *
     * @param keyId
     * @return the security provider (which is never {@code null})
     * @throws NullPointerException if the provided key ID is {@code null}
     */
    Provider getProvider(String keyId);
}
