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

package org.apache.cxf.systest.outofband.header;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import org.apache.cxf.BusFactory;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;

import org.apache.cxf.outofband.header.ObjectFactory;
import org.apache.cxf.outofband.header.OutofBandHeader;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType;
import org.apache.hello_world_doc_lit_bare.SOAPService;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;
import org.junit.BeforeClass;
import org.junit.Test;


public class OOBHeaderTest extends AbstractBusClientServerTestBase {    

    public static final String CONFIG_FILE = "org/apache/cxf/systest/outofband/header/cxf.xml";
    
    public static final String TEST_HDR_NS = "http://cxf.apache.org/outofband/Header";
    public static final String TEST_HDR_REQUEST_ELEM = "outofbandHeader";
    public static final String TEST_HDR_RESPONSE_ELEM = "outofbandHeader";
    
    private final QName serviceName = new QName("http://apache.org/hello_world_doc_lit_bare",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_doc_lit_bare", "SoapPort");
    
    
    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("org.apache.cxf.bus.factory", "org.apache.cxf.bus.CXFBusFactory");
        System.setProperty("cxf.config.file", "org/apache/cxf/systest/outofband/header/cxf.xml");
        
        defaultConfigFileName = CONFIG_FILE;
        SpringBusFactory bf = new SpringBusFactory();
        staticBus = bf.createBus(defaultConfigFileName);
        BusFactory.setDefaultBus(staticBus);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    private void addOutOfBoundHeader(PutLastTradedPricePortType portType, boolean invalid, boolean mu) {
        InvocationHandler handler  = Proxy.getInvocationHandler(portType);
        BindingProvider  bp = null;

        try {
            if (handler instanceof BindingProvider) {
                bp = (BindingProvider)handler;
                Map<String, Object> requestContext = bp.getRequestContext();
                
                OutofBandHeader ob = new OutofBandHeader();
                ob.setName("testOobHeader");
                ob.setValue("testOobHeaderValue");
                ob.setHdrAttribute(invalid ? "dontProcess" : "testHdrAttribute");

                SoapHeader hdr = new SoapHeader(
                        new QName(TEST_HDR_NS, TEST_HDR_REQUEST_ELEM), 
                        ob, 
                        new JAXBDataBinding(ob.getClass()));
                hdr.setMustUnderstand(mu);

                List<Header> holder = new ArrayList<Header>();
                holder.add(hdr);
                
                //Add List of headerHolders to requestContext.
                requestContext.put(Header.HEADER_LIST, holder); 
            }
        } catch (JAXBException ex) {
            //System.out.println("failed to insert header into request context :" + ex);
        }
        
    }
    
    private void checkReturnedOOBHeader(PutLastTradedPricePortType portType) {
        InvocationHandler handler  = Proxy.getInvocationHandler(portType);
        BindingProvider  bp = null;
        if (handler instanceof BindingProvider) {
            bp = (BindingProvider)handler;
            Map<String, Object> responseContext = bp.getResponseContext();
            OutofBandHeader hdrToTest = null;
            List oobHdr = (List) responseContext.get(Header.HEADER_LIST);
            if (oobHdr == null) {
                fail("Should have got List of out-of-band headers ..");
            }
            
            assertTrue("HeaderHolder list expected to conain 1 object received " + oobHdr.size(), 
                    oobHdr.size() == 1);
         
            if (oobHdr != null & oobHdr instanceof List) {
                Iterator iter = oobHdr.iterator();
                while (iter.hasNext()) {
                    Object hdr = iter.next();
                    if (hdr instanceof Header) {
                        Header hdr1 = (Header) hdr;
                        if (hdr1.getObject() instanceof Node) {
                            //System.out.println("Node conains : " + hdr1.getObject().toString());
                            try {
                                JAXBElement job = (JAXBElement) JAXBContext.newInstance(ObjectFactory.class)
                                    .createUnmarshaller()
                                    .unmarshal((Node) hdr1.getObject());
                                hdrToTest = (OutofBandHeader) job.getValue();
//                                 System.out.println("oob-hdr contains : \nname = " 
//                                       + hdrToTest.getName() 
//                                       + "  \nvalue = " + hdrToTest.getValue() 
//                                       + " \natribute = " + hdrToTest.getHdrAttribute());
                            } catch (JAXBException ex) {
                                //
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
            
            assertNotNull("out-of-band header should not be null", hdrToTest);

            assertTrue("Expected out-of-band Header name testOobReturnHeaderName recevied :" 
                    + hdrToTest.getName(), 
                    "testOobReturnHeaderName".equals(hdrToTest.getName()));
            assertTrue("Expected out-of-band Header value testOobReturnHeaderValue recevied :" 
                        + hdrToTest.getValue(), 
                        "testOobReturnHeaderValue".equals(hdrToTest.getValue()));
            assertTrue("Expected out-of-band Header attribute testReturnHdrAttribute recevied :" 
                        + hdrToTest.getHdrAttribute(), 
                        "testReturnHdrAttribute".equals(hdrToTest.getHdrAttribute())); 
        }
    }
    
    @Test
    public void testBasicConnection() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/doc_lit_bare.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        PutLastTradedPricePortType putLastTradedPrice = service.getPort(portName,
            PutLastTradedPricePortType.class);        
                
        TradePriceData priceData = new TradePriceData();
        priceData.setTickerPrice(1.0f);
        priceData.setTickerSymbol("CELTIX");

        assertTrue(check(0, putLastTradedPrice, false, true, priceData));
        assertFalse(check(1, putLastTradedPrice, false, true, priceData));
        assertTrue(check(2, putLastTradedPrice, false, true, priceData));        

        assertFalse(check(0, putLastTradedPrice, true, true, priceData));
        assertFalse(check(1, putLastTradedPrice, true, true, priceData));
        assertFalse(check(2, putLastTradedPrice, true, true, priceData));        

        assertTrue(check(0, putLastTradedPrice, false, false, priceData));
        assertTrue(check(1, putLastTradedPrice, false, false, priceData));
        assertTrue(check(2, putLastTradedPrice, false, false, priceData));        

        assertTrue(check(0, putLastTradedPrice, true, false, priceData));
        assertTrue(check(1, putLastTradedPrice, true, false, priceData));
        assertTrue(check(2, putLastTradedPrice, true, false, priceData));        
    }
    
    private boolean check(int i, PutLastTradedPricePortType putLastTradedPrice, 
                       boolean invalid, boolean mu,
                       TradePriceData priceData) {
        String address = "";
        switch (i) {
        case 0:
            address = "http://localhost:9107/SOAPDocLitBareService/SoapPort";
            break;
        case 1:
            address = "http://localhost:9107/SOAPDocLitBareService/SoapPortNoHeader";
            break;
        default:
            address = "http://localhost:9107/SOAPDocLitBareService/SoapPortHeader";                
        }
        ((BindingProvider)putLastTradedPrice).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        
        Holder<TradePriceData> holder = new Holder<TradePriceData>(priceData);
        try {
            addOutOfBoundHeader(putLastTradedPrice, invalid, mu);
            putLastTradedPrice.sayHi(holder);
            checkReturnedOOBHeader(putLastTradedPrice);
            return true;
        } catch (SOAPFaultException ex) {
            if (ex.getMessage().contains("MustUnderstand")) {
                return false;
            }
            throw ex;
        }
    }
}
