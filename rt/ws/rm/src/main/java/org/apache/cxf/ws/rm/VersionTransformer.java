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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.rm.v200702.AcceptType;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Expires;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.OfferType;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

// Importation convention: only the 200702 namespace versions of classes (the standard version used throughout
// the code) are imported directly. All other versions are fully qualified.

/**
 * This class is responsible for transforming between the native WS-ReliableMessaging schema version
 * (currently http://docs.oasis-open.org/ws-rx/wsrm/200702) and exposed version (which may be the 200702
 * namespace, the http://schemas.xmlsoap.org/ws/2005/02/rm namespace using the old
 * http://schemas.xmlsoap.org/ws/2004/08/addressing WS-Addressing namespace, or 2005/02 namespace with the
 * newer http://www.w3.org/2005/08/addressing WS-Addressing namespace).
 * <p>
 * The native version is that used throughout the stack, where the WS-RM types are represented via the JAXB
 * generated types.
 * <p>
 * The exposed version is that used when the WS-RM types are externalized, i.e. are encoded in the headers of
 * outgoing messages. For outgoing requests, the exposed version is  determined from configuration. For
 * outgoing responses, the exposed version is determined by the exposed version of the corresponding request.
 */
public final class VersionTransformer {

    /**
     * Constructor.
     */
    private VersionTransformer() {
    }

    /**
     * Check if a namespace URI represents a supported version of WS-ReliableMessaging.
     *
     * @param uri
     * @return <code>true</code> if supported, <code>false</code> if not
     */
    public static boolean isSupported(String uri) {
        return RM10Constants.NAMESPACE_URI.equals(uri)
            || RM11Constants.NAMESPACE_URI.equals(uri);
    }

    /**
     * Convert CreateSequenceType to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static
    org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceType convert200502wsa15(CreateSequenceType internal) {
        org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createCreateSequenceType();
        exposed.setAcksTo(internal.getAcksTo());
        exposed.setExpires(convert200502wsa15(internal.getExpires()));
        exposed.setOffer(convert200502wsa15(internal.getOffer()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert CreateSequenceType to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.CreateSequenceType convert200502(CreateSequenceType internal) {
        org.apache.cxf.ws.rm.v200502.CreateSequenceType exposed =
            RMUtils.getWSRM200502Factory().createCreateSequenceType();
        exposed.setAcksTo(org.apache.cxf.ws.addressing.VersionTransformer.convert(internal.getAcksTo()));
        exposed.setExpires(convert200502(internal.getExpires()));
        exposed.setOffer(convert200502(internal.getOffer()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:Expires to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502wsa15.Expires convert200502wsa15(Expires internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502wsa15.Expires exposed =
            RMUtils.getWSRM200502WSA200508Factory().createExpires();
        exposed.setValue(internal.getValue());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:Expires to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502.Expires convert200502(Expires internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502.Expires exposed = RMUtils.getWSRM200502Factory().createExpires();
        exposed.setValue(internal.getValue());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:Identifier to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.Identifier convert200502wsa15(Identifier internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502wsa15.Identifier exposed =
            RMUtils.getWSRM200502WSA200508Factory().createIdentifier();
        exposed.setValue(internal.getValue());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:Identifier to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static org.apache.cxf.ws.rm.v200502.Identifier convert200502(Identifier internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502.Identifier exposed = RMUtils.getWSRM200502Factory().createIdentifier();
        exposed.setValue(internal.getValue());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert 200502 wsrm:Identifier with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static Identifier convert(org.apache.cxf.ws.rm.v200502wsa15.Identifier exposed) {
        if (exposed == null) {
            return null;
        }
        Identifier internal = new Identifier();
        internal.setValue(exposed.getValue());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:Identifier with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static Identifier convert(org.apache.cxf.ws.rm.v200502.Identifier exposed) {
        if (exposed == null) {
            return null;
        }
        Identifier internal = new Identifier();
        internal.setValue(exposed.getValue());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:CreateSequenceType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static CreateSequenceType convert(org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceType exposed) {
        if (exposed == null) {
            return null;
        }
        CreateSequenceType internal = new CreateSequenceType();
        internal.setAcksTo(exposed.getAcksTo());
        internal.setExpires(convert(exposed.getExpires()));
        internal.setOffer(convert(exposed.getOffer()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:CreateSequenceType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static CreateSequenceType convert(org.apache.cxf.ws.rm.v200502.CreateSequenceType exposed) {
        if (exposed == null) {
            return null;
        }
        CreateSequenceType internal = new CreateSequenceType();
        internal.setAcksTo(org.apache.cxf.ws.addressing.VersionTransformer.convert(exposed.getAcksTo()));
        internal.setExpires(convert(exposed.getExpires()));
        internal.setOffer(convert(exposed.getOffer()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:CreateSequenceResponseType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static CreateSequenceResponseType
    convert(org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceResponseType exposed) {
        if (exposed == null) {
            return null;
        }
        CreateSequenceResponseType internal = new CreateSequenceResponseType();
        internal.setAccept(convert(exposed.getAccept()));
        internal.setExpires(convert(exposed.getExpires()));
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:CreateSequenceResponseType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if exposed is <code>null</code>)
     */
    public static CreateSequenceResponseType
    convert(org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType exposed) {
        if (exposed == null) {
            return null;
        }
        CreateSequenceResponseType internal = new CreateSequenceResponseType();
        internal.setAccept(convert(exposed.getAccept()));
        internal.setExpires(convert(exposed.getExpires()));
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert wsrm:Offer to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502wsa15.OfferType convert200502wsa15(OfferType internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502wsa15.OfferType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createOfferType();
        exposed.setExpires(convert200502wsa15(internal.getExpires()));
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:Offer to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502.OfferType convert200502(OfferType internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502.OfferType exposed = RMUtils.getWSRM200502Factory().createOfferType();
        exposed.setExpires(convert200502(internal.getExpires()));
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:CreateSequenceResponseType to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceResponseType
    convert200502wsa15(CreateSequenceResponseType internal) {
        org.apache.cxf.ws.rm.v200502wsa15.CreateSequenceResponseType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createCreateSequenceResponseType();
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        exposed.setExpires(convert200502wsa15(internal.getExpires()));
        exposed.setAccept(convert200502wsa15(internal.getAccept()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:CreateSequenceResponseType to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType
    convert200502(CreateSequenceResponseType internal) {
        org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType exposed =
            RMUtils.getWSRM200502Factory().createCreateSequenceResponseType();
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        exposed.setExpires(convert200502(internal.getExpires()));
        exposed.setAccept(convert200502(internal.getAccept()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:AcceptType to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502wsa15.AcceptType convert200502wsa15(AcceptType internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502wsa15.AcceptType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createAcceptType();
        exposed.setAcksTo(internal.getAcksTo());
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:AcceptType to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    private static org.apache.cxf.ws.rm.v200502.AcceptType convert200502(AcceptType internal) {
        if (internal == null) {
            return null;
        }
        org.apache.cxf.ws.rm.v200502.AcceptType exposed = RMUtils.getWSRM200502Factory().createAcceptType();
        exposed.setAcksTo(org.apache.cxf.ws.addressing.VersionTransformer.convert(internal.getAcksTo()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:SequenceType to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.SequenceType convert200502wsa15(SequenceType internal) {
        org.apache.cxf.ws.rm.v200502wsa15.SequenceType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createSequenceType();
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        exposed.setMessageNumber(internal.getMessageNumber());
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert 200502 wsrm:SequenceType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static SequenceType convert(org.apache.cxf.ws.rm.v200502.SequenceType exposed) {
        SequenceType internal = new SequenceType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        internal.setMessageNumber(exposed.getMessageNumber());
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:SequenceType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static SequenceType convert(org.apache.cxf.ws.rm.v200502wsa15.SequenceType exposed) {
        SequenceType internal = new SequenceType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        internal.setMessageNumber(exposed.getMessageNumber());
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 Expires with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static Expires convert(org.apache.cxf.ws.rm.v200502.Expires exposed) {
        if (exposed == null) {
            return null;
        }
        Expires internal = new Expires();
        internal.setValue(exposed.getValue());
        return internal;
    }

    /**
     * Convert 200502 Expires with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static Expires convert(org.apache.cxf.ws.rm.v200502wsa15.Expires exposed) {
        if (exposed == null) {
            return null;
        }
        Expires internal = new Expires();
        internal.setValue(exposed.getValue());
        return internal;
    }

    /**
     * Convert 200502 AcceptType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static AcceptType convert(org.apache.cxf.ws.rm.v200502.AcceptType exposed) {
        if (exposed == null) {
            return null;
        }
        AcceptType internal = new AcceptType();
        internal.setAcksTo(org.apache.cxf.ws.addressing.VersionTransformer.convert(exposed.getAcksTo()));
        return internal;
    }

    /**
     * Convert 200502 AcceptType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static AcceptType convert(org.apache.cxf.ws.rm.v200502wsa15.AcceptType exposed) {
        if (exposed == null) {
            return null;
        }
        AcceptType internal = new AcceptType();
        internal.setAcksTo(exposed.getAcksTo());
        return internal;
    }

    /**
     * Convert 200502 OfferType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static OfferType convert(org.apache.cxf.ws.rm.v200502.OfferType exposed) {
        if (exposed == null) {
            return null;
        }
        OfferType internal = new OfferType();
        internal.setExpires(convert(exposed.getExpires()));
        internal.setIdentifier(convert(exposed.getIdentifier()));
        return internal;
    }

    /**
     * Convert 200502 OfferType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (may be <code>null</code>)
     * @return converted (<code>null</code> if internal is <code>null</code>)
     */
    public static OfferType convert(org.apache.cxf.ws.rm.v200502wsa15.OfferType exposed) {
        if (exposed == null) {
            return null;
        }
        OfferType internal = new OfferType();
        internal.setExpires(convert(exposed.getExpires()));
        internal.setIdentifier(convert(exposed.getIdentifier()));
        return internal;
    }

    /**
     * Convert wsrm:SequenceType to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.SequenceType convert200502(SequenceType internal) {
        org.apache.cxf.ws.rm.v200502.SequenceType exposed =
            RMUtils.getWSRM200502Factory().createSequenceType();
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        exposed.setMessageNumber(internal.getMessageNumber());
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert TerminateSequenceType to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.TerminateSequenceType
    convert200502(TerminateSequenceType internal) {
        org.apache.cxf.ws.rm.v200502.TerminateSequenceType exposed =
            new org.apache.cxf.ws.rm.v200502.TerminateSequenceType();
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert TerminateSequenceType to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.TerminateSequenceType
    convert200502wsa15(TerminateSequenceType internal) {
        org.apache.cxf.ws.rm.v200502wsa15.TerminateSequenceType exposed =
            new org.apache.cxf.ws.rm.v200502wsa15.TerminateSequenceType();
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:SequenceAcknowledgement to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement
    convert200502wsa15(SequenceAcknowledgement internal) {
        org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement exposed =
            RMUtils.getWSRM200502WSA200508Factory().createSequenceAcknowledgement();
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        List<org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement.AcknowledgementRange> exposedRanges
            = exposed.getAcknowledgementRange();
        for (SequenceAcknowledgement.AcknowledgementRange range : internal.getAcknowledgementRange()) {
            org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement.AcknowledgementRange exposedRange
                = new org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement.AcknowledgementRange();
            exposedRange.setLower(range.getLower());
            exposedRange.setUpper(range.getUpper());
            exposedRanges.add(exposedRange);
            putAll(range.getOtherAttributes(), exposedRange.getOtherAttributes());
        }
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:SequenceAcknowledgement to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement
    convert200502(SequenceAcknowledgement internal) {
        org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement exposed =
            RMUtils.getWSRM200502Factory().createSequenceAcknowledgement();
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        List<org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement.AcknowledgementRange> exposedRanges
            = exposed.getAcknowledgementRange();
        for (SequenceAcknowledgement.AcknowledgementRange range : internal.getAcknowledgementRange()) {
            org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement.AcknowledgementRange exposedRange
                = new org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement.AcknowledgementRange();
            exposedRange.setLower(range.getLower());
            exposedRange.setUpper(range.getUpper());
            exposedRanges.add(exposedRange);
            putAll(range.getOtherAttributes(), exposedRange.getOtherAttributes());
        }
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert 200502 wsrm:SequenceAcknowledgement with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static SequenceAcknowledgement
    convert(org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement exposed) {
        SequenceAcknowledgement internal = new SequenceAcknowledgement();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        List<SequenceAcknowledgement.AcknowledgementRange> internalRanges
            = internal.getAcknowledgementRange();
        for (org.apache.cxf.ws.rm.v200502wsa15.SequenceAcknowledgement.AcknowledgementRange range
            : exposed.getAcknowledgementRange()) {
            SequenceAcknowledgement.AcknowledgementRange internalRange
                = new SequenceAcknowledgement.AcknowledgementRange();
            internalRange.setLower(range.getLower());
            internalRange.setUpper(range.getUpper());
            internalRanges.add(internalRange);
            putAll(range.getOtherAttributes(), internalRange.getOtherAttributes());
        }
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:SequenceAcknowledgement with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static SequenceAcknowledgement
    convert(org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement exposed) {
        SequenceAcknowledgement internal = new SequenceAcknowledgement();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        List<SequenceAcknowledgement.AcknowledgementRange> internalRanges
            = internal.getAcknowledgementRange();
        for (org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement.AcknowledgementRange range
            : exposed.getAcknowledgementRange()) {
            SequenceAcknowledgement.AcknowledgementRange internalRange
                = new SequenceAcknowledgement.AcknowledgementRange();
            internalRange.setLower(range.getLower());
            internalRange.setUpper(range.getUpper());
            internalRanges.add(internalRange);
            putAll(range.getOtherAttributes(), internalRange.getOtherAttributes());
        }
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert wsrm:SequenceAcknowledgement to 200502 version with 200508 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.AckRequestedType
    convert200502wsa15(AckRequestedType internal) {
        org.apache.cxf.ws.rm.v200502wsa15.AckRequestedType exposed =
            RMUtils.getWSRM200502WSA200508Factory().createAckRequestedType();
        exposed.setIdentifier(convert200502wsa15(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert wsrm:SequenceAcknowledgement to 200502 version with 200408 WS-Addressing namespace.
     *
     * @param internal (non-<code>null</code>)
     * @return converted
     */
    public static org.apache.cxf.ws.rm.v200502.AckRequestedType
    convert200502(AckRequestedType internal) {
        org.apache.cxf.ws.rm.v200502.AckRequestedType exposed =
            RMUtils.getWSRM200502Factory().createAckRequestedType();
        exposed.setIdentifier(convert200502(internal.getIdentifier()));
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert 200502 wsrm:SequenceAcknowledgement with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static AckRequestedType
    convert(org.apache.cxf.ws.rm.v200502wsa15.AckRequestedType exposed) {
        AckRequestedType internal = new AckRequestedType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 wsrm:SequenceAcknowledgement with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static AckRequestedType
    convert(org.apache.cxf.ws.rm.v200502.AckRequestedType exposed) {
        AckRequestedType internal = new AckRequestedType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 TerminateSequenceType with 200508 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static TerminateSequenceType
    convert(org.apache.cxf.ws.rm.v200502wsa15.TerminateSequenceType exposed) {
        TerminateSequenceType internal = new TerminateSequenceType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Convert 200502 TerminateSequenceType with 200408 WS-Addressing namespace to internal form.
     *
     * @param exposed (non-<code>null</code>)
     * @return converted
     */
    public static TerminateSequenceType
    convert(org.apache.cxf.ws.rm.v200502.TerminateSequenceType exposed) {
        TerminateSequenceType internal = new TerminateSequenceType();
        internal.setIdentifier(convert(exposed.getIdentifier()));
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return internal;
    }

    /**
     * Put all entries from one map into another.
     * @param from source map
     * @param to target map
     */
    private static void putAll(Map<QName, String> from, Map<QName, String> to) {
        if (from != null) {
            to.putAll(from);
        }
    }

    /**
     * Add all entries from one list into another.
     * @param from source list
     * @param to target list
     */
    private static void addAll(List<Object> from, List<Object> to) {
        if (from != null) {
            to.addAll(from);
        }
    }
}