package org.apache.cxf.rs.security.httpsignature;

import java.security.PublicKey;

@FunctionalInterface
public interface PublicKeyProvider {

    /**
     *
     * @param keyId
     * @return the public key (which is never {@code null})
     * @throws NullPointerException if the provided key ID is {@code null}
     */
    PublicKey getKey(String keyId);

}
