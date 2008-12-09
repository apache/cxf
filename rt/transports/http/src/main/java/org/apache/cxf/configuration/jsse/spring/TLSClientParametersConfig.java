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
package org.apache.cxf.configuration.jsse.spring;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;


import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.TLSClientParametersType;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * This class provides the TLSClientParameters that programmatically
 * configure a HTTPConduit. It is initialized with the JAXB
 * type TLSClientParametersType that was used in the Spring configuration
 * of the http-conduit bean.
 */
public final class TLSClientParametersConfig {
    static JAXBContext context;
    
    private TLSClientParametersConfig() {
        //not constructed
    }
    
    private static synchronized JAXBContext getContext() throws JAXBException {
        if (context == null) {
            context = JAXBContext.newInstance(PackageUtils.getPackageName(TLSClientParametersType.class), 
                                              TLSClientParametersConfig.class.getClassLoader());            
        }
        return context;
    }

    static TLSClientParameters createTLSClientParametersFromType(TLSClientParametersType params) 
        throws GeneralSecurityException,
               IOException {

        TLSClientParameters ret = new TLSClientParameters();
        if (params.isDisableCNCheck()) {
            ret.setDisableCNCheck(true);
        }
        if (params.isSetCipherSuitesFilter()) {
            ret.setCipherSuitesFilter(params.getCipherSuitesFilter());
        }
        if (params.isSetCipherSuites()) {
            ret.setCipherSuites(params.getCipherSuites().getCipherSuite());
        }
        if (params.isSetJsseProvider()) {
            ret.setJsseProvider(params.getJsseProvider());
        }
        if (params.isSetSecureRandomParameters()) {
            ret.setSecureRandom(
                TLSParameterJaxBUtils.getSecureRandom(
                        params.getSecureRandomParameters()));
        }
        if (params.isSetKeyManagers()) {
            ret.setKeyManagers(
                TLSParameterJaxBUtils.getKeyManagers(params.getKeyManagers()));
        }
        if (params.isSetTrustManagers()) {
            ret.setTrustManagers(
                TLSParameterJaxBUtils.getTrustManagers(
                        params.getTrustManagers()));
        }
        return ret;
    }
    


    public static Object createTLSClientParameters(String s) {
        
        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        Unmarshaller u;
        try {
            u = getContext().createUnmarshaller();
            Object obj = u.unmarshal(data, TLSClientParametersType.class);
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();

            }
            
            TLSClientParametersType cpt = (TLSClientParametersType)obj;
            return createTLSClientParametersFromType(cpt);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
