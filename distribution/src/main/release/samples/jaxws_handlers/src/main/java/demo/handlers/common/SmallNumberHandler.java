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

package demo.handlers.common;

import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import org.apache.handlers.types.AddNumbers;
import org.apache.handlers.types.AddNumbersResponse;
import org.apache.handlers.types.ObjectFactory;



/**
 * handles addition of small numbers.
 */
public class SmallNumberHandler implements LogicalHandler<LogicalMessageContext> {


    // Implementation of javax.xml.ws.handler.Handler

    public final boolean handleMessage(LogicalMessageContext messageContext) {
        System.out.println("LogicalMessageHandler handleMessage called");

        try {
            boolean outbound = (Boolean)messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            if (outbound) {
                // get the LogicalMessage from our context
                //
                LogicalMessage msg = messageContext.getMessage();

                // check the payload, if its an AddNumbers request, we'll intervene
                //
                JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
                Object payload = msg.getPayload(jaxbContext);
                if (payload instanceof JAXBElement) {
                    payload = ((JAXBElement)payload).getValue();
                }

                if (payload instanceof AddNumbers) {
                    AddNumbers req = (AddNumbers)payload;

                    // now, if the arguments are small, let's do the calculation here
                    //
                    int a = req.getArg0();
                    int b = req.getArg1();

                    if (isSmall(a) && isSmall(b)) {
                        int answer = a + b;

                        //System.out.printf("SmallNumberHandler addNumbers(%d, %d) == %d\n", a, b, answer);
                        // ok, we've done the calculation, so build the
                        // response and set it as the payload of the message
                        
                        AddNumbersResponse resp = new AddNumbersResponse();
                        resp.setReturn(answer);
                        msg.setPayload(new ObjectFactory().createAddNumbersResponse(resp),
                                       jaxbContext);
                        
                        Source src = msg.getPayload();                                             
                        msg.setPayload(src);
                        
                        payload = msg.getPayload(jaxbContext);
                        if (payload instanceof JAXBElement) {
                            payload = ((JAXBElement)payload).getValue();
                        }
                        
                        AddNumbersResponse resp2 = (AddNumbersResponse)payload;
                        if (resp2 == resp) {
                            throw new WebServiceException("Shouldn't be the same object");
                        }

                        // finally, return false, indicating that request
                        // processing stops here and our answer will be
                        // returned to the client
                        return false;
                    }
                }
            }
            return true;
        } catch (JAXBException ex) {
            throw new ProtocolException(ex);
        }

    }

    public final boolean handleFault(LogicalMessageContext messageContext) {
        System.out.println("LogicalMessageHandler handleFault called");
        System.out.println(messageContext);

        return true;
    }

    public void close(MessageContext ctx) {
        System.out.println("LogicalHandler close called");
    }

    public void init(Map config) {
        System.out.println("LogicalHandler init called");
    }

    public void destroy() {
        System.out.println("LogicalHandler close called");
    }

    private boolean isSmall(int i) {
        return i > 0 && i <= 10;
    }
}
