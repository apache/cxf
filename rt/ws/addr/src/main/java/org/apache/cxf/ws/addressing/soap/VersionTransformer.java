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

package org.apache.cxf.ws.addressing.soap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.addressing.v200408.Relationship;

/**
 * This class is responsible for transforming between the native WS-Addressing schema version (i.e. 2005/08)
 * and exposed version (currently may be 2005/08 or 2004/08).
 * <p>
 * The native version is that used throughout the stack, were the WS-A types are represented via the JAXB
 * generated types for the 2005/08 schema.
 * <p>
 * The exposed version is that used when the WS-A types are externalized, i.e. are encoded in the headers of
 * outgoing messages. For outgoing requests, the exposed version is determined from configuration. For
 * outgoing responses, the exposed version is determined by the exposed version of the corresponding request.
 * <p>
 * The motivation for using different native and exposed types is usually to facilitate a WS-* standard based
 * on an earlier version of WS-Adressing (for example WS-RM depends on the 2004/08 version).
 */
public class VersionTransformer extends org.apache.cxf.ws.addressing.VersionTransformer {

    public static final Set<QName> HEADERS;
    private static final Logger LOG = LogUtils.getL7dLogger(VersionTransformer.class);

    protected MAPCodec codec;

    /**
     * Constructor.
     *
     * @param mapCodec the MAPCodec to use
     */
    public VersionTransformer(MAPCodec mapCodec) {
        codec = mapCodec;
    }

    /**
     * Encode message in exposed version.
     *
     * @param message The message to be encoded
     * @param exposeAs specifies the WS-Addressing version to expose
     * @param value the value to encode
     * @param localName the localName for the header
     * @param clz the class
     * @param marshaller the JAXB context to use
     * @param mustUnderstand whether mustUnderstand is true
     */
    public <T> void encodeAsExposed(SoapMessage message,
                                    String exposeAs, T value,
                                    String localName, Class<T> clz,
                                    JAXBContext marshaller,
                                    boolean mustUnderstand) throws JAXBException {
        if (value != null) {
            if (NATIVE_VERSION.equals(exposeAs)) {
                codec.encodeMAP(message, value, new QName(exposeAs, localName), clz,
                                marshaller, mustUnderstand);
            } else if (org.apache.cxf.ws.addressing.VersionTransformer.Names200408
                            .WSA_NAMESPACE_NAME.equals(exposeAs)) {
                if (AttributedURIType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convert((AttributedURIType)value), new QName(exposeAs, localName),
                                    AttributedURI.class, marshaller, mustUnderstand);
                } else if (EndpointReferenceType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convert((EndpointReferenceType)value), new QName(exposeAs, localName),
                                    org.apache.cxf.ws.addressing.VersionTransformer.Names200408.EPR_TYPE,
                                    marshaller, mustUnderstand);
                } else if (RelatesToType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convert((RelatesToType)value), new QName(exposeAs, localName),
                                    Relationship.class, marshaller, mustUnderstand);
                }
            } else if (org.apache.cxf.ws.addressing.VersionTransformer.Names200403
                            .WSA_NAMESPACE_NAME.equals(exposeAs)) {
                if (AttributedURIType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convertTo200403((AttributedURIType)value),
                                    new QName(exposeAs, localName),
                                    org.apache.cxf.ws.addressing.v200403.AttributedURI.class,
                                    marshaller, mustUnderstand);
                } else if (EndpointReferenceType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convertTo200403((EndpointReferenceType)value), new QName(exposeAs,
                                                                                             localName),
                                    org.apache.cxf.ws.addressing.VersionTransformer.Names200403.EPR_TYPE,
                                    marshaller, mustUnderstand);
                } else if (RelatesToType.class.equals(clz)) {
                    codec.encodeMAP(message,
                                    convertTo200403((RelatesToType)value), new QName(exposeAs, localName),
                                    org.apache.cxf.ws.addressing.v200403.Relationship.class,
                                    marshaller, mustUnderstand);
                }
            }
        }
    }

    /**
     * Decodes a MAP from a exposed version.
     *
     * @param encodedAs specifies the encoded version
     * @param clz the class
     * @param headerElement the SOAP header element
     * @param unmarshaller the JAXB unmarshaller to use
     * @return the decoded value
     */
    public <T> T decodeAsNative(String encodedAs, Class<T> clz, Element headerElement,
                                Unmarshaller unmarshaller) throws JAXBException {
        T ret = null;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("decodeAsNative: encodedAs: " + encodedAs);
            LOG.fine("                class: " + clz.getName());
        }

        if (NATIVE_VERSION.equals(encodedAs)) {
            ret = codec.decodeMAP(clz, headerElement, unmarshaller);
        } else if (Names200408.WSA_NAMESPACE_NAME.equals(encodedAs)) {
            if (AttributedURIType.class.equals(clz)) {
                ret = clz.cast(convert(codec.decodeMAP(AttributedURI.class, headerElement, unmarshaller)));
            } else if (EndpointReferenceType.class.equals(clz)) {
                ret = clz.cast(convert(codec.decodeMAP(Names200408.EPR_TYPE,
                                                       headerElement, unmarshaller)));
            } else if (RelatesToType.class.equals(clz)) {
                ret = clz.cast(convert(codec.decodeMAP(Relationship.class, headerElement, unmarshaller)));
            }
        } else if (org.apache.cxf.ws.addressing.VersionTransformer.Names200403.WSA_NAMESPACE_NAME.equals(encodedAs)) {
            if (AttributedURIType.class.equals(clz)) {
                ret = clz.cast(convert(codec
                    .decodeMAP(org.apache.cxf.ws.addressing.v200403.AttributedURI.class, headerElement,
                               unmarshaller)));
            } else if (EndpointReferenceType.class.equals(clz)) {
                ret = clz.cast(convert(codec.decodeMAP(Names200403.EPR_TYPE,
                                                       headerElement, unmarshaller)));
            } else if (RelatesToType.class.equals(clz)) {
                ret = clz.cast(convert(codec
                    .decodeMAP(org.apache.cxf.ws.addressing.v200403.Relationship.class, headerElement,
                               unmarshaller)));
            }
        }
        return ret;
    }

    /**
     * Augment the set of headers understood by the protocol binding with the 2004/08 header QNames.
     */
    static {
        Set<QName> headers = new HashSet<>();
        headers.addAll(Names.HEADERS);

        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_FROM_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_TO_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_REPLYTO_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_FAULTTO_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_ACTION_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_MESSAGEID_NAME));
        headers.add(new QName(Names200408.WSA_NAMESPACE_NAME, Names.WSA_RELATESTO_NAME));

        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_FROM_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_TO_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_REPLYTO_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_FAULTTO_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_ACTION_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_MESSAGEID_NAME));
        headers.add(new QName(Names200403.WSA_NAMESPACE_NAME, Names.WSA_RELATESTO_NAME));

        HEADERS = Collections.unmodifiableSet(headers);
    }

}
