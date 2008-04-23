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

package org.apache.cxf.systest.versioning;


import java.io.InputStream;
import java.util.Set;



import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AbstractEndpointSelectionInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;


public class MediatorInInterceptor extends AbstractEndpointSelectionInterceptor {

    public MediatorInInterceptor() {
        super(Phase.POST_STREAM);
        addBefore(StaxInInterceptor.class.getName());
    }

    @Override
    protected Endpoint selectEndpoint(Message message, Set<Endpoint> eps) {
        
        InputStream is = message.getContent(InputStream.class);        
        if (is == null) {
            return null;
        }
        //cache the input stream
        CachedOutputStream bos = new CachedOutputStream(4096);
        try {
            IOUtils.copy(is, bos);
            is.close();
            
            message.setContent(InputStream.class, bos.getInputStream());
        
            String encoding = (String)message.get(Message.ENCODING);

            XMLStreamReader xsr;
            xsr = StaxInInterceptor.getXMLInputFactory(message).
                createXMLStreamReader(bos.getInputStream(), encoding);
            
            // move to the soap body            
            while (true) {                
                xsr.nextTag();              
                
                if ("Body".equals(xsr.getName().getLocalPart())) {
                    break;
                }
            }
            
            xsr.nextTag();
            if (!xsr.isStartElement()) {
                return null;
            }
            
            String methodName = xsr.getName().getLocalPart();
            //if the incoming message has a element containes "SayHi", we redirect the message
            //to the new version of service on endpoint "local://localhost:9027/SoapContext/version2/SoapPort"
            for (Endpoint ep : eps) {               
                if (methodName.indexOf("sayHi") != -1) {
                    if ("2".equals(ep.get("version"))) {
                        return ep;
                    }
                } else if ("1".equals(ep.get("version"))) {
                    return ep;
                }
            }
            bos.close();            
        } catch (Exception e) {
            throw new Fault(e);
        }    
        return null;
    }

}
