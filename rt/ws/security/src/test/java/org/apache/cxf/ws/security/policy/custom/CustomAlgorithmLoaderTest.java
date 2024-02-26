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
package org.apache.cxf.ws.security.policy.custom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CustomAlgorithmLoaderTest {

    private static final Map<String, Object> CUSTOMIZATION_PARAMETERS = new HashMap<>();


    static {
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_DIGEST_ALGORITHM, "digestAlg");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_ALGORITHM, "encryptAlg");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_SYMMETRIC_KEY_ENCRYPTION_ALGORITHM,
                "symmEncryptAlg");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_ASYMMETRIC_KEY_ENCRYPTION_ALGORITHM,
                "asymmEncryptAlg");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_SIGNATURE_KEY_DERIVATION,
                "signatureKeyDerivation");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_KEY_DERIVATION,
                "encryptKeyDerivation");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_SYMMETRIC_SIGNATURE, "symmetricSignature");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_ASYMMETRIC_SIGNATURE, "asymmetricSignature");
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_DERIVED_KEY_LENGTH, 111);
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_SIGNATURE_DERIVED_KEY_LENGTH, 222);
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_MINIMUM_SYMMETRIC_KEY_LENGTH, 333);
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_MAXIMUM_SYMMETRIC_KEY_LENGTH, 444);
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_MINIMUM_ASYMMETRIC_KEY_LENGTH, 555);
        CUSTOMIZATION_PARAMETERS.put(SecurityConstants.CUSTOM_ALG_SUITE_MAXIMUM_ASYMMETRIC_KEY_LENGTH, 666);
    }

    @Test
    public void customizeNullAlgSuite() throws Exception {

        AlgorithmSuite.AlgorithmSuiteType custom =
                DefaultAlgorithmSuiteLoader.customize(null, Collections.emptyMap());

        assertNull(custom);
    }

    @Test
    public void customizeNullAlgSuiteWithParams() throws Exception {

        AlgorithmSuite.AlgorithmSuiteType custom =
                DefaultAlgorithmSuiteLoader.customize(null, CUSTOMIZATION_PARAMETERS);

        assertNull(custom);
    }


    @Test
    public void customize()  {

        AlgorithmSuite.AlgorithmSuiteType toCustomize = createRealAlgSuite("CustomAlgorithmSuite");

        AlgorithmSuite.AlgorithmSuiteType custom =
                DefaultAlgorithmSuiteLoader.customize(toCustomize, CUSTOMIZATION_PARAMETERS);

        assertNotNull(custom);
        assertSuiteType(createFullyCustomSuite(), custom);
    }

    @Test
    public void customizeWithNoParams()  {

        AlgorithmSuite.AlgorithmSuiteType toCustomize = createRealAlgSuite("CustomAlgorithmSuite");
        AlgorithmSuite.AlgorithmSuiteType origValues = createRealAlgSuite("not_required");


        AlgorithmSuite.AlgorithmSuiteType custom =
                DefaultAlgorithmSuiteLoader.customize(toCustomize, Collections.emptyMap());

        assertNotNull(custom);
        //values have to be same as original
        assertSuiteType(origValues, custom);
    }

    @Test
    public void customizeDifferentAlgSuite()  {

        AlgorithmSuite.AlgorithmSuiteType toCustomize = createRealAlgSuite("differentName");
        AlgorithmSuite.AlgorithmSuiteType origValues = createRealAlgSuite("not_required");


        AlgorithmSuite.AlgorithmSuiteType custom =
                DefaultAlgorithmSuiteLoader.customize(toCustomize, CUSTOMIZATION_PARAMETERS);

        assertNotNull(custom);
        //if suite to be custom is not named "CustomAlgorithmSuite", values has to be same as original
        assertSuiteType(origValues, custom);
    }
    
    private void assertSuiteType(AlgorithmSuite.AlgorithmSuiteType expected,
                                 AlgorithmSuite.AlgorithmSuiteType custom) {
        assertEquals(expected.getDigest(), custom.getDigest());
        assertEquals(expected.getEncryption(), custom.getEncryption());
        assertEquals(expected.getSymmetricKeyWrap(), custom.getSymmetricKeyWrap());
        assertEquals(expected.getAsymmetricKeyWrap(), custom.getAsymmetricKeyWrap());
        assertEquals(expected.getEncryptionKeyDerivation(), custom.getEncryptionKeyDerivation());
        assertEquals(expected.getSignatureKeyDerivation(), custom.getSignatureKeyDerivation());
        assertEquals(expected.getSymmetricSignature(), custom.getSymmetricSignature());
        assertEquals(expected.getAsymmetricSignature(), custom.getAsymmetricSignature());
        assertEquals(expected.getEncryptionDerivedKeyLength(), custom.getEncryptionDerivedKeyLength());
        assertEquals(expected.getSignatureDerivedKeyLength(), custom.getSignatureDerivedKeyLength());
        assertEquals(expected.getMinimumSymmetricKeyLength(), custom.getMinimumSymmetricKeyLength());
        assertEquals(expected.getMaximumSymmetricKeyLength(), custom.getMaximumSymmetricKeyLength());
        assertEquals(expected.getMinimumAsymmetricKeyLength(), custom.getMinimumAsymmetricKeyLength());
        assertEquals(expected.getMaximumAsymmetricKeyLength(), custom.getMaximumAsymmetricKeyLength());
        assertEquals(expected.getEncryptionDigest(), custom.getEncryptionDigest());
    }


    private static AlgorithmSuite.AlgorithmSuiteType createRealAlgSuite(String name) {
        return new AlgorithmSuite.AlgorithmSuiteType(
                name,
                SPConstants.SHA1,
                "http://www.w3.org/2009/xmlenc11#aes256-gcm",
                SPConstants.KW_AES256,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L256,
                SPConstants.P_SHA1_L192,
                256, 192, 256, 256, 1024, 4096
        );
    }

    private static AlgorithmSuite.AlgorithmSuiteType createFullyCustomSuite() {
        return new AlgorithmSuite.AlgorithmSuiteType(
                "CustomAlgorithmSuite",
                "digestAlg",
                "encryptAlg",
                "symmEncryptAlg",
                "asymmEncryptAlg",
                "encryptKeyDerivation",
                "signatureKeyDerivation",
                "symmetricSignature",
                "asymmetricSignature",
                111, 222, 333, 444, 555, 666
        );
    }
}