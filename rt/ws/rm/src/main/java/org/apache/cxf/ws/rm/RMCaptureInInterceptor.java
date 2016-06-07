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

package org.apache.cxf.ws.rm;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.OutTransformWriter;
import org.apache.cxf.ws.addressing.AddressingProperties;

/**
 * 
 */
public class RMCaptureInInterceptor extends AbstractRMInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(RMCaptureInInterceptor.class);

    public RMCaptureInInterceptor() {
        super(Phase.POST_STREAM);
        addAfter(StaxInInterceptor.class.getName());
    }

    @Override
    protected void handle(Message message) throws SequenceFault, RMException {
       
        // all messages are initially captured as they cannot be distinguished at this phase
        // Non application messages temp files are released (cos.releaseTempFileHold()) in RMInInterceptor
        if (!MessageUtils.isTrue(message.getContextualProperty(Message.ROBUST_ONEWAY))
            && (getManager().getStore() != null || (getManager().getDestinationPolicy() != null && getManager()
                .getDestinationPolicy().getRetryPolicy() != null))) {

            message.getInterceptorChain().add(new RMCaptureInEnd());
            XMLStreamReader reader = message.getContent(XMLStreamReader.class);
            
            if (null != reader) {
                CachedOutputStream saved = new CachedOutputStream();
                // REVISIT check factory for READER
                try {
                    StaxUtils.copy(reader, saved);
                    saved.flush();
                    saved.holdTempFile();
                    reader.close();
                    LOG.fine("Create new XMLStreamReader");
                    InputStream is = saved.getInputStream();
                    // keep References to clean-up tmp files in RMDeliveryInterceptor
                    setCloseable(message, saved, is);
                    XMLStreamReader newReader = StaxUtils.createXMLStreamReader(is);
                    StaxUtils.configureReader(reader, message);
                    message.setContent(XMLStreamReader.class, newReader);
                    LOG.fine("Capturing the original RM message");
                    message.put(RMMessageConstants.SAVED_CONTENT, saved);
                } catch (XMLStreamException | IOException e) {
                    throw new Fault(e);
                }
            } else {
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                                  "No message found for redeliver", LOG, Collections.<String> emptyList());
                RMException ex = new RMException(msg);
                throw new Fault(ex);
            }
        }
    }

    private boolean isApplicationMessage(Message message) {
        final AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);
        if (null != maps && null != maps.getAction()) {
            return !RMContextUtils.isRMProtocolMessage(maps.getAction().getValue());
        }
        return false;
    }

    private void setCloseable(Message message, CachedOutputStream cos, InputStream is) {
        message.put("org.apache.cxf.ws.rm.content.closeable", new Closeable() {
            @Override
            public void close() throws IOException {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
                try {
                    cos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        });
    }

    /**
     * RMCaptureInEnd interceptor is used to switch saved_content, in case WSS is activated.
     */
    private class RMCaptureInEnd extends AbstractPhaseInterceptor<Message> {
        RMCaptureInEnd() {
            super(Phase.PRE_LOGICAL);
            addBefore(RMInInterceptor.class.getName());
        }

        @Override
        public void handleFault(Message message) {
            // in case of a SequenceFault SAVED_CONTENT must be released
            Exception ex = message.getContent(Exception.class);
            if (ex instanceof SequenceFault) {
                Closeable closable = (Closeable)message.get("org.apache.cxf.ws.rm.content.closeable");
                if (null != closable) {
                    try {
                        closable.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                CachedOutputStream saved = (CachedOutputStream)message.get(RMMessageConstants.SAVED_CONTENT);
                if (saved != null) {
                    saved.releaseTempFileHold();
                    try {
                        saved.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        public void handleMessage(Message message) {
            LOG.entering(getClass().getName(), "handleMessage");
            // Capturing the soap envelope. In case of WSS was activated, decrypted envelope is captured.
            if (!MessageUtils.isTrue(message.getContextualProperty(Message.ROBUST_ONEWAY))
                && isApplicationMessage(message)
                && (getManager().getStore() != null || (getManager().getDestinationPolicy() != null && getManager()
                    .getDestinationPolicy().getRetryPolicy() != null))) {

                CachedOutputStream saved = new CachedOutputStream();
                SOAPMessage soapMessage = message.getContent(SOAPMessage.class);

                if (soapMessage != null) {
                    try {
                        javax.xml.transform.Source envelope = soapMessage.getSOAPPart().getContent();
                        StaxUtils.copy(envelope, saved);
                        saved.flush();
                        // create a new source part from cos
                        InputStream is = saved.getInputStream();
                        // close old saved content
                        closeOldSavedContent(message);
                        // keep References to clean-up tmp files in RMDeliveryInterceptor
                        setCloseable(message, saved, is);
                        StreamSource source = new StreamSource(is);
                        soapMessage.getSOAPPart().setContent(source);
                        // when WSS was activated, saved content still contains soap headers to be removed
                        message.put(RMMessageConstants.SAVED_CONTENT, removeUnnecessarySoapHeaders(saved));
                    } catch (SOAPException | IOException | XMLStreamException e) {
                        throw new Fault(e);
                    }
                }
            }
        }

        private void closeOldSavedContent(Message message) {
            CachedOutputStream saved = (CachedOutputStream)message.get(RMMessageConstants.SAVED_CONTENT);
            if (saved != null) {
                saved.releaseTempFileHold();
                try {
                    saved.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            Closeable closable = (Closeable)message.get("org.apache.cxf.ws.rm.content.closeable");
            if (null != closable) {
                try {
                    closable.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        private CachedOutputStream removeUnnecessarySoapHeaders(CachedOutputStream saved) {
            CachedOutputStream newSaved = new CachedOutputStream();

            InputStream is = null;
            try {
                is = saved.getInputStream();
                XMLStreamWriter capture = StaxUtils.createXMLStreamWriter(newSaved,
                                                                          StandardCharsets.UTF_8.name());
                Map<String, String> map = new HashMap<String, String>();
                map.put("{http://schemas.xmlsoap.org/ws/2005/02/rm}Sequence", "");
                map.put("{http://schemas.xmlsoap.org/ws/2005/02/rm}SequenceAcknowledgement", "");
                map.put("{http://docs.oasis-open.org/ws-rx/wsrm/200702}Sequence", "");
                map.put("{http://docs.oasis-open.org/ws-rx/wsrm/200702}SequenceAcknowledgement", "");
                map.put("{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security",
                        "");
                // attributes to be removed
                Map<String, String> amap = new HashMap<String, String>();
                amap.put("{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Id",
                         "");

                capture = new OutTransformWriter(capture, map, Collections.<String, String> emptyMap(),
                                                 Collections.<String> emptyList(), amap, false, null);
                StaxUtils.copy(new StreamSource(is), capture);
                capture.flush();
                capture.close();
                newSaved.flush();
                // hold temp file, otherwise it will be deleted in case msg was written to RMTxStore
                // or resend was executed
                newSaved.holdTempFile();
                is.close();
            } catch (IOException | XMLStreamException e) {
                throw new Fault(e);
            } finally {
                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
            return newSaved;
        }
    }
}
