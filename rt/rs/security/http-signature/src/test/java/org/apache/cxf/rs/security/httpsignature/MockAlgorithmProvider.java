package org.apache.cxf.rs.security.httpsignature;

public class MockAlgorithmProvider implements AlgorithmProvider {

    @Override
    public String getAlgorithmName(String keyId) {
        return "rsa-sha256";
    }
}
