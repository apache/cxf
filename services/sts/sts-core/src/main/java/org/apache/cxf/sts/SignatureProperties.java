/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.sts;

import java.util.ArrayList;
import java.util.List;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.dom.WSConstants;

/**
 * This class contains various configuration properties that can be used to sign an issued token,
 * or generate a symmetric key in the STS.
 */
public class SignatureProperties {
    private String signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private String c14nAlgorithm = WSConstants.C14N_EXCL_OMIT_COMMENTS;
    private List<String> acceptedSignatureAlgorithms = new ArrayList<>();
    private List<String> acceptedC14nAlgorithms = new ArrayList<>();
    private boolean useKeyValue;
    private long keySize = 256;
    private long minimumKeySize = 128;
    private long maximumKeySize = 512;
    private String digestAlgorithm = WSConstants.SHA256;

    public SignatureProperties() {
        // Default signature algorithms
        acceptedSignatureAlgorithms.add(WSS4JConstants.RSA_SHA1);
        acceptedSignatureAlgorithms.add(signatureAlgorithm);
        acceptedSignatureAlgorithms.add("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512");

        // Default c14n algorithms
        acceptedC14nAlgorithms.add(c14nAlgorithm);
    }

    /**
     * Get whether a KeyValue is used to refer to a a certificate used to sign an issued token.
     * The default is false.
     */
    public boolean isUseKeyValue() {
        return useKeyValue;
    }

    /**
     * Set whether a KeyValue is used to refer to a a certificate used to sign an issued token.
     * The default is false.
     */
    public void setUseKeyValue(boolean useKeyValue) {
        this.useKeyValue = useKeyValue;
    }

    /**
     * Get the key size to use when generating a symmetric key. The default is 256 bits.
     */
    public long getKeySize() {
        return keySize;
    }

    /**
     * Set the key size to use when generating a symmetric key. The default is
     * 256 bits.
     */
    public void setKeySize(long keySize) {
        this.keySize = keySize;
    }

    /**
     * Get the minimum key size to use when generating a symmetric key. The requestor can
     * specify a KeySize value to use. The default is 128 bits.
     */
    public long getMinimumKeySize() {
        return minimumKeySize;
    }

    /**
     * Set the minimum key size to use when generating a symmetric key. The requestor can
     * specify a KeySize value to use. The default is 128 bits.
     */
    public void setMinimumKeySize(long minimumKeySize) {
        this.minimumKeySize = minimumKeySize;
    }

    /**
     * Get the maximum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 512 bits.
     */
    public long getMaximumKeySize() {
        return maximumKeySize;
    }

    /**
     * Set the maximum key size to use when generating a symmetric key to sign an issued token. The
     * requestor can specify a KeySize value to use. The default is 512 bits.
     */
    public void setMaximumKeySize(long maximumKeySize) {
        this.maximumKeySize = maximumKeySize;
    }

    /**
     * Get the signature algorithm to use
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Set the signature algorithm to use
     */
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * Get the c14n algorithm to use
     */
    public String getC14nAlgorithm() {
        return c14nAlgorithm;
    }

    /**
     * Set the c14n algorithm to use
     */
    public void setC14nAlgorithm(String c14nAlgorithm) {
        this.c14nAlgorithm = c14nAlgorithm;
    }

    /**
     * Get the list of accepted signature algorithms. A request can contain a wst:SignatureAlgorithm
     * uri to use to sign an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are RSA-SHA1.
     */
    public List<String> getAcceptedSignatureAlgorithms() {
        return acceptedSignatureAlgorithms;
    }

    /**
     * Set the list of accepted signature algorithms. A request can contain a wst:SignatureAlgorithm
     * uri to use to sign an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are RSA-SHA1.
     */
    public void setAcceptedSignatureAlgorithms(
        List<String> acceptedSignatureAlgorithms
    ) {
        this.acceptedSignatureAlgorithms = acceptedSignatureAlgorithms;
    }


    /**
     * Get the list of accepted c14n algorithms. A request can contain a wst:CanonicalizationAlgorithm
     * uri to use for c14n in an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are C14N_EXCL_OMIT_COMMENTS.
     */
    public List<String> getAcceptedC14nAlgorithms() {
        return acceptedC14nAlgorithms;
    }

    /**
     * Set the list of accepted c14n algorithms. A request can contain a wst:CanonicalizationAlgorithm
     * uri to use for c14n in an issued token. The algorithm specified must be contained in this list.
     * The default algorithms are C14N_EXCL_OMIT_COMMENTS.
     */
    public void setAcceptedC14nAlgorithms(List<String> acceptedC14nAlgorithms) {
        this.acceptedC14nAlgorithms = acceptedC14nAlgorithms;
    }

    /**
     * Get the Digest algorithm to use for Signature
     * @return the Digest algorithm to use for Signature
     */
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * Set the Digest algorithm to use for Signature
     * @param digestAlgorithm the Digest algorithm to use for Signature
     */
    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

}
