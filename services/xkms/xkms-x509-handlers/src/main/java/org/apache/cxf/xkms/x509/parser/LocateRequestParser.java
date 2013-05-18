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
package org.apache.cxf.xkms.x509.parser;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.xkms.exception.XKMSException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.apache.cxf.xkms.model.xmldsig.X509IssuerSerialType;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public final class LocateRequestParser {

    private LocateRequestParser() {
    }

    public static List<UseKeyWithType> parse(LocateRequestType request) {
        List<UseKeyWithType> keyIDs = new ArrayList<UseKeyWithType>();
        if (request == null) {
            return keyIDs;
        }

        QueryKeyBindingType query = request.getQueryKeyBinding();
        if (query == null) {
            return keyIDs;
        }

        // http://www.w3.org/TR/xkms2/ [213]
        if (query.getTimeInstant() != null) {
            throw new XKMSException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_TIME_INSTANT_NOT_SUPPORTED);
        }

        keyIDs.addAll(parse(query.getKeyInfo()));

        List<UseKeyWithType> useKeyList = query.getUseKeyWith();
        keyIDs.addAll(useKeyList);

        return keyIDs;
    }

    protected static List<UseKeyWithType> parse(KeyInfoType keyInfo) {
        List<UseKeyWithType> keyIDs = new ArrayList<UseKeyWithType>();

        if (keyInfo == null) {
            return keyIDs;
        }

        List<Object> content = keyInfo.getContent();
        for (Object obj1 : content) {
            if (obj1 instanceof JAXBElement) {
                JAXBElement<?> keyInfoChild = (JAXBElement<?>) obj1;
                if (X509Utils.X509_KEY_NAME.equals(keyInfoChild.getName())) {
                    UseKeyWithType keyDN = new UseKeyWithType();
                    keyDN.setApplication(Applications.PKIX.getUri());
                    keyDN.setIdentifier((String) keyInfoChild.getValue());
                    keyIDs.add(keyDN);

                } else if (X509Utils.X509_DATA.equals(keyInfoChild.getName())) {
                    X509DataType x509Data = (X509DataType) keyInfoChild.getValue();
                    List<Object> x509DataContent = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();

                    for (Object obj2 : x509DataContent) {
                        if (obj2 instanceof JAXBElement) {
                            JAXBElement<?> x509DataChild = (JAXBElement<?>) obj2;

                            if (X509Utils.X509_ISSUER_SERIAL.equals(x509DataChild.getName())) {
                                X509IssuerSerialType x509IssuerSerial = (X509IssuerSerialType) x509DataChild.getValue();

                                UseKeyWithType issuer = new UseKeyWithType();
                                issuer.setApplication(Applications.ISSUER.getUri());
                                issuer.setIdentifier(x509IssuerSerial.getX509IssuerName());
                                keyIDs.add(issuer);

                                UseKeyWithType serial = new UseKeyWithType();
                                serial.setApplication(Applications.SERIAL.getUri());
                                serial.setIdentifier(x509IssuerSerial.getX509SerialNumber().toString());
                                keyIDs.add(serial);

                            } else if (X509Utils.X509_SUBJECT_NAME.equals(x509DataChild.getName())) {
                                UseKeyWithType keyDN = new UseKeyWithType();
                                keyDN.setApplication(Applications.PKIX.getUri());
                                keyDN.setIdentifier((String) x509DataChild.getValue());
                                keyIDs.add(keyDN);
                            }
                        }
                    }
                }
            }
        }
        return keyIDs;
    }
}
