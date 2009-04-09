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

package org.apache.cxf.systest.ws.security;

import java.io.IOException;
import java.math.BigInteger;

import javax.jws.WebService;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.policytest.doubleit.DoubleItPortType;
import org.apache.cxf.policytest.doubleit.DoubleItService;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.junit.BeforeClass;
import org.junit.Test;


public class SecurityPolicyTest extends AbstractBusClientServerTestBase  {
    public static final String POLICY_ADDRESS = "http://localhost:9010/SecPolTest";
    public static final String POLICY_HTTPS_ADDRESS = "https://localhost:9009/SecPolTest";
    public static final String POLICY_ENCSIGN_ADDRESS = "http://localhost:9010/SecPolTestEncryptThenSign";
    public static final String POLICY_SIGNENC_ADDRESS = "http://localhost:9010/SecPolTestSignThenEncrypt";
    public static final String POLICY_SIGN_ADDRESS = "http://localhost:9010/SecPolTestSign";

    
    public static class ServerPasswordCallback implements CallbackHandler {
        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            if (pc.getIdentifier().equals("bob")) {
                // set the password on the callback. This will be compared to the
                // password which was sent from the client.
                pc.setPassword("pwd");
            }
        }
    }
    
    
    
    @BeforeClass 
    public static void init() throws Exception {
        
        createStaticBus(SecurityPolicyTest.class.getResource("https_config.xml").toString())
            .getExtension(PolicyEngine.class).setEnabled(true);
        getStaticBus().getOutInterceptors().add(new LoggingOutInterceptor());
        EndpointImpl ep = (EndpointImpl)Endpoint.publish(POLICY_HTTPS_ADDRESS,
                                       new DoubleItImplHttps());
        ep.getServer().getEndpoint().getEndpointInfo().setProperty(SecurityConstants.CALLBACK_HANDLER,
                                                                   new ServerPasswordCallback());
        Endpoint.publish(POLICY_ADDRESS,
                         new DoubleItImpl());
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_ENCSIGN_ADDRESS,
                                            new DoubleItImplEncryptThenSign());
        
        EndpointInfo ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGNENC_ADDRESS,
                                            new DoubleItImplSignThenEncrypt());
        
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGN_ADDRESS,
                                            new DoubleItImplSign());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
    }
    
    @Test
    public void testPolicy() throws Exception {
        DoubleItService service = new DoubleItService();
        DoubleItPortType pt;

        pt = service.getDoubleItPortEncryptThenSign();
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));
        
        pt = service.getDoubleItPortSign();
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));

        
        pt = service.getDoubleItPortSignThenEncrypt();
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));
        
        pt = service.getDoubleItPortHttps();
        try {
            pt.doubleIt(BigInteger.valueOf(25));
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!msg.contains("sername")) {
                throw ex;
            }
        }
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.PASSWORD, "pwd");
        pt.doubleIt(BigInteger.valueOf(25));
        
        try {
            pt = service.getDoubleItPortHttp();
            pt.doubleIt(BigInteger.valueOf(25));
            fail("https policy should have triggered");
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!msg.contains("HttpsToken")) {
                throw ex;
            }
        }

        
    }
    
    
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortHttp",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest/DoubleIt.wsdl")
    public static class DoubleItImpl implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) {
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortHttps",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest/DoubleIt.wsdl")
    public static class DoubleItImplHttps implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) {
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortEncryptThenSign",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest/DoubleIt.wsdl")
    public static class DoubleItImplEncryptThenSign implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) {
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortSignThenEncrypt",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest/DoubleIt.wsdl")
    public static class DoubleItImplSignThenEncrypt implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) {
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortSign",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest/DoubleIt.wsdl")
    public static class DoubleItImplSign implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) {
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
}
