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

package org.apache.cxf.transport.jbi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public class JBIConduitOutputStream extends CachedOutputStream {

    private static final Logger LOG = LogUtils.getL7dLogger(JBIConduitOutputStream.class);

    private Message message;
    private boolean isOneWay;
    private DeliveryChannel channel;
    private JBIConduit conduit;
    private EndpointReferenceType target;

    public JBIConduitOutputStream(Message m, DeliveryChannel channel, EndpointReferenceType target,
                                  JBIConduit conduit) {
        message = m;
        this.channel = channel;
        this.conduit = conduit;
        this.target = target;
        
    }

    @Override
    protected void doFlush() throws IOException {

    }

    @Override
    protected void doClose() throws IOException {
        isOneWay = message.getExchange().isOneWay();
        commitOutputMessage();
        if (target != null) {
            target.getClass();
        }
    }

    private void commitOutputMessage() throws IOException {
        try {
            Member member = (Member) message.get(Method.class.getName());
            Class<?> clz = member.getDeclaringClass();
            Exchange exchange = message.getExchange();
            BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);

            LOG.info(new org.apache.cxf.common.i18n.Message("INVOKE.SERVICE", LOG).toString() + clz);

            WebService ws = clz.getAnnotation(WebService.class);
            assert ws != null;
            QName interfaceName = new QName(ws.targetNamespace(), ws.name());
            QName serviceName = null;
            if (target != null) {
                serviceName = EndpointReferenceUtils.getServiceName(target,
                                                                    message.getExchange().get(Bus.class));
            } else {
                serviceName = message.getExchange().get(org.apache.cxf.service.Service.class).getName();
            }
          
            MessageExchangeFactory factory = channel.createExchangeFactoryForService(serviceName);
            LOG.info(new org.apache.cxf.common.i18n.Message("CREATE.MESSAGE.EXCHANGE", LOG).toString()
                     + serviceName);
            MessageExchange xchng = null;
            if (isOneWay) {
                xchng = factory.createInOnlyExchange();
            } else if (bop.getOutput() == null) {
                xchng = factory.createRobustInOnlyExchange();
            } else {
                xchng = factory.createInOutExchange();
            }

            NormalizedMessage inMsg = xchng.createMessage();
            LOG.info(new org.apache.cxf.common.i18n.Message("EXCHANGE.ENDPOINT", LOG).toString()
                     + xchng.getEndpoint());
            if (inMsg != null) {
                LOG.info("setup message contents on " + inMsg);
                inMsg.setContent(getMessageContent(message));
                xchng.setService(serviceName);
                LOG.info("service for exchange " + serviceName);

                xchng.setInterfaceName(interfaceName);

                xchng.setOperation(bop.getName());
                xchng.setMessage(inMsg, "in");
                LOG.info("sending message");
                if (!isOneWay) {
                    channel.sendSync(xchng);
                    NormalizedMessage outMsg = ((InOut)xchng).getOutMessage();
                    Source content = null;
                    if (outMsg != null) {
                        content = outMsg.getContent();
                    } else {
                        if (((InOut)xchng).getFault() == null) {
                            throw xchng.getError();
                        }
                        content = ((InOut)xchng).getFault().getContent();
                    }
                    Message inMessage = new MessageImpl();
                    message.getExchange().setInMessage(inMessage);
                    InputStream ins = JBIMessageHelper.convertMessageToInputStream(content);
                    if (ins == null) {
                        throw new IOException(new org.apache.cxf.common.i18n.Message(
                            "UNABLE.RETRIEVE.MESSAGE", LOG).toString());
                    }
                    inMessage.setContent(InputStream.class, ins);
                    inMessage.put(MessageExchange.class, xchng);
                    
                    
                    conduit.getMessageObserver().onMessage(inMessage);

                    xchng.setStatus(ExchangeStatus.DONE);
                    channel.send(xchng);                    
                } else {
                    channel.send(xchng);
                }

            } else {
                LOG.info(new org.apache.cxf.common.i18n.Message("NO.MESSAGE", LOG).toString());
            }

            

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    private Source getMessageContent(Message message2) throws IOException {
        return new StreamSource(this.getInputStream());
        
    }

    @Override
    protected void onWrite() throws IOException {

    }

}
