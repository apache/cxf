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

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Node;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.outofband.header.ObjectFactory;
import org.apache.cxf.outofband.header.OutofBandHeader;
import org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;




@javax.jws.WebService(serviceName = "SOAPService", 
                      portName = "SoapPort",
                      endpointInterface = "org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType",
                      targetNamespace = "http://apache.org/hello_world_doc_lit_bare",
                      wsdlLocation = "testutils/doc_lit_bare.wsdl")
public class OOBHdrServiceImpl implements PutLastTradedPricePortType {
    @Resource
    private WebServiceContext context;
    
    public void sayHi(Holder<TradePriceData> inout) {
        inout.value.setTickerPrice(4.5f);
        inout.value.setTickerSymbol("APACHE");
        if (checkContext()) {
            //System.out.println("Received out-of-band header as expected..");
            sendReturnOOBHeader();
        }
    }   
    
    private void sendReturnOOBHeader() {
        if (context != null) {
            MessageContext ctx = context.getMessageContext();
            if (ctx != null) {
                try {
//                  Create out-of-band header object.
                    OutofBandHeader ob = new OutofBandHeader();
                    ob.setName("testOobReturnHeaderName");
                    ob.setValue("testOobReturnHeaderValue");
                    ob.setHdrAttribute("testReturnHdrAttribute");
                    // Add Out-of-band header object to HeaderHolder.

                    JAXBElement<OutofBandHeader> job = new JAXBElement<OutofBandHeader>(
                            new QName(OOBHeaderTest.TEST_HDR_NS, OOBHeaderTest.TEST_HDR_RESPONSE_ELEM), 
                            OutofBandHeader.class, null, ob);
                    Header hdr = new Header(
                            new QName(OOBHeaderTest.TEST_HDR_NS, OOBHeaderTest.TEST_HDR_RESPONSE_ELEM), 
                            job, 
                            new JAXBDataBinding(ob.getClass()));
                    List<Header> hdrList = CastUtils.cast((List<?>) ctx.get(Header.HEADER_LIST));
                    hdrList.add(hdr);
                    //Add headerHolder to requestContext.
//                    ctx.put(Header.HEADER_LIST, hdrList);
                    //System.out.println("Completed adding list to context");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public void putLastTradedPrice(TradePriceData body) {
        //System.out.println("-----TradePriceData TickerPrice : ----- " + body.getTickerPrice());
        //System.out.println("-----TradePriceData TickerSymbol : ----- " + body.getTickerSymbol());
    }
    
    private boolean checkContext() {
        boolean success = false;
        MessageContext ctx = context == null ? null : context.getMessageContext();
        if (ctx.containsKey(Header.HEADER_LIST)) {
            List oobHdr = (List) ctx.get(Header.HEADER_LIST);
            Iterator iter = oobHdr.iterator();
            while (iter.hasNext()) {
                Object hdr = iter.next();
                if (hdr instanceof Header && ((Header) hdr).getObject() instanceof Node) {
                    Header hdr1 = (Header) hdr;
                    //System.out.println("Node conains : " + hdr1.getObject().toString());
                    try {
                        JAXBElement job = (JAXBElement) JAXBContext.newInstance(ObjectFactory.class)
                            .createUnmarshaller()
                            .unmarshal((Node) hdr1.getObject());
                        OutofBandHeader ob = (OutofBandHeader) job.getValue();
                        if ("testOobHeader".equals(ob.getName())
                            && "testOobHeaderValue".equals(ob.getValue())) { 
                            if ("testHdrAttribute".equals(ob.getHdrAttribute())) {
                                success = true;
                                iter.remove(); //mark it processed
                            } else if ("dontProcess".equals(ob.getHdrAttribute())) {
                                //we won't remove it so we won't let the runtime know
                                //it's processed.   It SHOULD throw an exception 
                                //saying the mustunderstand wasn't processed
                                success = true;
                            }
                        } else {
                            throw new RuntimeException("test failed");
                        }
                    } catch (JAXBException ex) {
                        //
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            throw new RuntimeException("MessageContext is null or doesnot contain OOBHeaders");
        }
        
        return success;
    }
    
    public String bareNoParam() {
        return "testResponse";
    }

    public String nillableParameter(BigDecimal theRequest) {
        return null;
    }
   

}
