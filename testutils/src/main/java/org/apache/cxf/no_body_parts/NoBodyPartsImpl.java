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

package org.apache.cxf.no_body_parts;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.no_body_parts.types.Operation1;
import org.apache.cxf.no_body_parts.types.Operation1Response;
import org.apache.cxf.no_body_parts.wsdl.NoBodyPartsSEI;

/**
 * Implementation class for NoBodyParts
 */
@WebService(targetNamespace = "urn:org:apache:cxf:no_body_parts/wsdl")
public class NoBodyPartsImpl implements NoBodyPartsSEI {

    private String md5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(bytes);
        byte messageDigest[] = algorithm.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        return hexString.toString();
    }

    /** {@inheritDoc} */
    public Operation1Response operation1(Operation1 parameters, Holder<byte[]> mimeAttachment) {
        Operation1Response r = new Operation1Response();
        try {
            r.setStatus(md5(mimeAttachment.value));
        } catch (NoSuchAlgorithmException e) {
            throw new WebServiceException(e);
        }
        return r;
    }
}
