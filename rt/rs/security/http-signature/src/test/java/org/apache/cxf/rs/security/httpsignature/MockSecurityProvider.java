package org.apache.cxf.rs.security.httpsignature;

import java.security.Provider;
import java.security.Security;

public class MockSecurityProvider implements SecurityProvider {
    public Provider getProvider(String keyId) {
        return Security.getProvider("SunRsaSign");
    }
}
