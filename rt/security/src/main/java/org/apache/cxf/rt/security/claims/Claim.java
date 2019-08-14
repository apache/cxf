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

package org.apache.cxf.rt.security.claims;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.logging.LogUtils;

/**
 * This represents a Claim.
 */
public class Claim implements Serializable, Cloneable {

    private static final long serialVersionUID = 5730726672368086795L;

    private static final Logger LOG = LogUtils.getL7dLogger(Claim.class);

    private String claimType;
    private boolean optional;
    private List<Object> values = new ArrayList<>(1);

    public Claim() {
    }

    /**
     * Create a clone of the provided claim.
     *
     * @param claim Claim to be cloned. Value cannot be null.
     */
    public Claim(Claim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim cannot be null");
        }
        claimType = claim.getClaimType();
        optional = claim.isOptional();
        values.addAll(claim.getValues());
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public void setClaimType(URI claimType) {
        this.claimType = claimType.toString();
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setValues(List<Object> values) {
        this.values.clear();
        this.values.addAll(values);
    }

    @Deprecated
    public void setValue(Object s) {
        addValue(s);
    }

    public void addValue(Object s) {
        this.values.add(s);
    }

    public List<Object> getValues() {
        return values;
    }

    public void serialize(XMLStreamWriter writer, String prefix, String namespace) throws XMLStreamException {
        String localname = "ClaimType";
        if (!values.isEmpty()) {
            localname = "ClaimValue";
        }
        writer.writeStartElement(prefix, localname, namespace);
        writer.writeAttribute(null, "Uri", claimType);
        if (optional) {
            writer.writeAttribute(null, "Optional", "true");
        }

        if (!values.isEmpty()) {
            for (Object value : values) {
                if (value instanceof String) {
                    writer.writeStartElement(prefix, "Value", namespace);
                    writer.writeCharacters((String)value);
                    writer.writeEndElement();
                } else {
                    LOG.warning("Only a ClaimValue String can be serialized");
                }
            }
        }
        writer.writeEndElement();
    }

    @Override
    public Claim clone() { //NOPMD
        try {
            super.clone(); // Checkstyle requires this call
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new Claim(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((claimType == null)
            ? 0
            : claimType.hashCode());
        result = prime * result + (optional
            ? 1231
            : 1237);
        result = prime * result + ((values == null)
            ? 0
            : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Claim)) {
            return false;
        }
        Claim other = (Claim)obj;
        if (claimType == null) {
            if (other.claimType != null) {
                return false;
            }
        } else if (!claimType.equals(other.claimType)) {
            return false;
        }
        if (optional != other.optional) {
            return false;
        }
        if (values == null) {
            if (other.values != null) {
                return false;
            }
        } else if (!values.equals(other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("Claim [values=");
        builder.append(values);
        builder.append(", claimType=");
        builder.append(claimType);
        builder.append(", optional=");
        builder.append(optional);
        builder.append(']');
        return builder.toString();
    }
}
