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

package org.apache.cxf.ws.rm.soap;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.BindingFaultFactory;
import org.apache.cxf.ws.rm.EncoderDecoder;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.SequenceFault;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 *
 */
public class SoapFaultFactory implements BindingFaultFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(SoapFaultFactory.class);

    private SoapVersion version;

    public SoapFaultFactory(Binding binding) {
        version = ((SoapBinding)binding).getSoapVersion();
    }

    public Fault createFault(SequenceFault sf, Message msg) {
        final Fault f;
        if (version == Soap11.getInstance()) {
            f = createSoap11Fault(sf);
            // so we can encode the SequenceFault as header
            f.initCause(sf);
        } else {
            f = createSoap12Fault(sf, msg);
        }
        return f;
    }

    Fault createSoap11Fault(SequenceFault sf) {
        return new SoapFault(sf.getReason(),
            sf.isSender() ? version.getSender() : version.getReceiver());
    }

    Fault createSoap12Fault(SequenceFault sf, Message msg) {
        SoapFault fault = (SoapFault)createSoap11Fault(sf);
        fault.setSubCode(sf.getFaultCode());
        Object detail = sf.getDetail();
        if (null == detail) {
            return fault;
        }

        try {
            RMProperties rmps = RMContextUtils.retrieveRMProperties(msg, false);
            AddressingProperties maps = RMContextUtils.retrieveMAPs(msg, false, false);
            EncoderDecoder codec = ProtocolVariation.findVariant(rmps.getNamespaceURI(),
                maps.getNamespaceURI()).getCodec();
            setDetail(fault, detail, codec);
        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "MARSHAL_FAULT_DETAIL_EXC", ex);
            ex.printStackTrace();
        }
        return fault;
    }

    void setDetail(SoapFault fault, Object detail, EncoderDecoder codec) throws Exception {
        String name = fault.getSubCode().getLocalPart();
        Element element = null;
        if (RMConstants.INVALID_ACKNOWLEDGMENT_FAULT_CODE.equals(name)) {
            element = codec.encodeSequenceAcknowledgement((SequenceAcknowledgement)detail);
        } else if (!RMConstants.CREATE_SEQUENCE_REFUSED_FAULT_CODE.equals(name)
            && !RMConstants.WSRM_REQUIRED_FAULT_CODE.equals(name)) {
            element = codec.encodeIdentifier((Identifier)detail);
        }
        fault.setDetail(element);
    }

    public String toString(Fault f) {
        SoapFault sf = (SoapFault)f;
        org.apache.cxf.common.i18n.Message msg
            = new org.apache.cxf.common.i18n.Message("SEQ_FAULT_MSG", LOG,
                new Object[] {sf.getReason(), sf.getFaultCode(), sf.getSubCode()});
        return msg.toString();
    }
}