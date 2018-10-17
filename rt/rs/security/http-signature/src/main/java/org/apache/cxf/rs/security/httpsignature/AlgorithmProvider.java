package org.apache.cxf.rs.security.httpsignature;

@FunctionalInterface
public interface AlgorithmProvider {

    /**
     *
     * @param keyId
     * @return the algorithm name (which is never {@code null})
     */
    String getAlgorithmName(String keyId);
}
