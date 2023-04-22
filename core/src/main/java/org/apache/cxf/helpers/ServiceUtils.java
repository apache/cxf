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

package org.apache.cxf.helpers;

import java.util.StringTokenizer;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;

public final class ServiceUtils {

    private ServiceUtils() {
    }

    /**
     * A short cut method to be able to test for if Schema Validation should be enabled
     * for IN or OUT without having to check BOTH and IN or OUT.
     *
     * @param message
     * @param type
     */
    public static boolean isSchemaValidationEnabled(SchemaValidationType type, Message message) {
        SchemaValidationType validationType = getSchemaValidationType(message);

        boolean isRequestor = MessageUtils.isRequestor(message);
        if (SchemaValidationType.REQUEST.equals(validationType)) {
            if (isRequestor) {
                validationType = SchemaValidationType.OUT;
            } else {
                validationType = SchemaValidationType.IN;
            }
        } else if (SchemaValidationType.RESPONSE.equals(validationType)) {
            if (isRequestor) {
                validationType = SchemaValidationType.IN;
            } else {
                validationType = SchemaValidationType.OUT;
            }
        }

        return validationType.equals(type)
            || ((SchemaValidationType.IN.equals(type) || SchemaValidationType.OUT.equals(type))
                && SchemaValidationType.BOTH.equals(validationType));
    }
    /**
     * A convenience method to check for schema validation config in the message context, and then in the service model.
     * Does not modify the Message context (other than what is done in the getContextualProperty itself)
     *
     * @param message
     */
    public static SchemaValidationType getSchemaValidationType(Message message) {
        SchemaValidationType validationType = getOverrideSchemaValidationType(message);
        if (validationType == null) {
            validationType = getSchemaValidationTypeFromModel(message);
        }
        if (validationType == null) {
            Object obj = message.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED);
            if (obj != null) {
                validationType = getSchemaValidationType(obj);
            }
        }
        if (validationType == null) {
            validationType = SchemaValidationType.NONE;
        }

        return validationType;
    }

    private static SchemaValidationType getOverrideSchemaValidationType(Message message) {
        Object obj = message.get(Message.SCHEMA_VALIDATION_ENABLED);
        if (obj == null && message.getExchange() != null) {
            obj = message.getExchange().get(Message.SCHEMA_VALIDATION_ENABLED);
        }
        if (obj != null) {
            // this method will transform the legacy enabled as well
            return getSchemaValidationType(obj);
        }
        return null;
    }

    private static SchemaValidationType getSchemaValidationTypeFromModel(Message message) {
        Exchange exchange = message.getExchange();
        SchemaValidationType validationType = null;

        if (exchange != null) {

            BindingOperationInfo boi = exchange.getBindingOperationInfo();
            if (boi != null) {
                OperationInfo opInfo = boi.getOperationInfo();
                if (opInfo != null) {
                    validationType = getSchemaValidationTypeFromModel(opInfo);
                }
            }

            if (validationType == null) {
                Endpoint endpoint = exchange.getEndpoint();
                if (endpoint != null) {
                    EndpointInfo ep = endpoint.getEndpointInfo();
                    if (ep != null) {
                        validationType = getSchemaValidationTypeFromModel(ep);
                    }
                }
            }
        }

        return validationType;
    }

    private static SchemaValidationType getSchemaValidationTypeFromModel(
        AbstractPropertiesHolder properties) {
        Object obj = properties.getProperty(Message.SCHEMA_VALIDATION_TYPE);
        if (obj != null) {
            return getSchemaValidationType(obj);
        }
        return null;
    }

    public static SchemaValidationType getSchemaValidationType(Object obj) {
        if (obj instanceof SchemaValidationType) {
            return (SchemaValidationType)obj;
        } else if (obj != null) {
            String value = obj.toString().toUpperCase(); // handle boolean values as well
            if ("TRUE".equals(value)) {
                return SchemaValidationType.BOTH;
            } else if ("FALSE".equals(value)) {
                return SchemaValidationType.NONE;
            } else if (value.length() > 0) {
                return SchemaValidationType.valueOf(value);
            }
        }

        // fall through default value
        return SchemaValidationType.NONE;
    }

    /**
     * Generates a suitable service name from a given class. The returned name
     * is the simple name of the class, i.e. without the package name.
     *
     * @param clazz the class.
     * @return the name.
     */
    public static String makeServiceNameFromClassName(Class<?> clazz) {
        String name = clazz.getName();
        int last = name.lastIndexOf('.');
        if (last != -1) {
            name = name.substring(last + 1);
        }

        int inner = name.lastIndexOf('$');
        if (inner != -1) {
            name = name.substring(inner + 1);
        }

        return name;
    }

    /**
     * Generates the name of a XML namespace from a given class name and
     * protocol. The returned namespace will take the form
     * <code>protocol://domain</code>, where <code>protocol</code> is the
     * given protocol, and <code>domain</code> the inversed package name of
     * the given class name. <p/> For instance, if the given class name is
     * <code>org.codehaus.xfire.services.Echo</code>, and the protocol is
     * <code>http</code>, the resulting namespace would be
     * <code>http://services.xfire.codehaus.org</code>.
     *
     * @param className the class name
     * @param protocol the protocol (eg. <code>http</code>)
     * @return the namespace
     */
    public static String makeNamespaceFromClassName(String className, String protocol) {
        int index = className.lastIndexOf('.');

        if (index == -1) {
            return protocol + "://" + "DefaultNamespace";
        }

        String packageName = className.substring(0, index);

        StringTokenizer st = new StringTokenizer(packageName, ".");
        String[] words = new String[st.countTokens()];

        for (int i = words.length - 1; i >= 0; --i) {
            words[i] = st.nextToken();
        }

        return protocol + "://" + String.join(".", words) + "/";
    }

}
