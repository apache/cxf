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

package org.apache.cxf.ws.security.sts.provider;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.CancelOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.cxf.ws.security.sts.provider.operation.KeyExchangeTokenOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RequestCollectionOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;

@ServiceMode(value = Service.Mode.PAYLOAD)
public class SecurityTokenServiceProvider implements Provider<Source> {

    private static final String WSTRUST_13_NAMESPACE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    private static final String WSTRUST_REQUESTTYPE_ELEMENTNAME = "RequestType";
    private static final String WSTRUST_REQUESTTYPE_ISSUE = WSTRUST_13_NAMESPACE
            + "/Issue";
    private static final String WSTRUST_REQUESTTYPE_CANCEL = WSTRUST_13_NAMESPACE
            + "/Cancel";
    private static final String WSTRUST_REQUESTTYPE_RENEW = WSTRUST_13_NAMESPACE
            + "/Renew";
    private static final String WSTRUST_REQUESTTYPE_VALIDATE = WSTRUST_13_NAMESPACE
            + "/Validate";
    private static final String WSTRUST_REQUESTTYPE_REQUESTCOLLECTION = WSTRUST_13_NAMESPACE
            + "/RequestCollection";
    private static final String WSTRUST_REQUESTTYPE_KEYEXCHANGETOKEN = WSTRUST_13_NAMESPACE
            + "/KeyExchangeToken";
    
    private static final Map<String, Method> OPERATION_METHODS = new HashMap<String, Method>();
    static {
        try {
            Method m = IssueOperation.class.getDeclaredMethod("issue", 
                                                              RequestSecurityTokenType.class, 
                                                              WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_ISSUE, m);
            
            m = CancelOperation.class.getDeclaredMethod("cancel", 
                                                       RequestSecurityTokenType.class, 
                                                       WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_CANCEL, m);
            
            m = RenewOperation.class.getDeclaredMethod("renew", 
                                                       RequestSecurityTokenType.class, 
                                                       WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_RENEW, m);
            
            m = ValidateOperation.class.getDeclaredMethod("validate", 
                                                       RequestSecurityTokenType.class, 
                                                       WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_VALIDATE, m);
            
            m = KeyExchangeTokenOperation.class.getDeclaredMethod("keyExchangeToken", 
                                                       RequestSecurityTokenType.class, 
                                                       WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_KEYEXCHANGETOKEN, m);
            
            m = RequestCollectionOperation.class.getDeclaredMethod("requestCollection", 
                                                       RequestSecurityTokenCollectionType.class, 
                                                       WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_REQUESTCOLLECTION, m);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    

    
    protected JAXBContext jaxbContext;
    protected Set<Class<?>> jaxbContextClasses;
    
    private CancelOperation cancelOperation;
    private IssueOperation issueOperation;
    private IssueSingleOperation issueSingleOperation;
    private KeyExchangeTokenOperation keyExchangeTokenOperation;
    private RenewOperation renewOperation;
    private RequestCollectionOperation requestCollectionOperation;
    private ValidateOperation validateOperation;
    private Map<String, Object> operationMap = new HashMap<String, Object>();

    @Resource
    private WebServiceContext context;

    public SecurityTokenServiceProvider() throws Exception {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ObjectFactory.class);
        classes.add(org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);
                
        CachedContextAndSchemas cache = 
            JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
        jaxbContext = cache.getContext();
        jaxbContextClasses = cache.getClasses();
    }
    
    public void setCancelOperation(CancelOperation cancelOperation) {
        this.cancelOperation = cancelOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_CANCEL, cancelOperation);
    }

    public void setIssueOperation(IssueOperation issueOperation) {
        this.issueOperation = issueOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_ISSUE, issueOperation);
    }
    
    /**
     * Setting an IssueSingleOperation instance will override the default behaviour of issuing
     * a token in a RequestSecurityTokenResponseCollection
     */
    public void setIssueSingleOperation(IssueSingleOperation issueSingleOperation) {
        this.issueSingleOperation = issueSingleOperation;
        Method m;
        try {
            m = IssueSingleOperation.class.getDeclaredMethod("issueSingle", 
                    RequestSecurityTokenType.class, 
                    WebServiceContext.class);
            OPERATION_METHODS.put(WSTRUST_REQUESTTYPE_ISSUE, m);
            operationMap.put(WSTRUST_REQUESTTYPE_ISSUE, issueSingleOperation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setKeyExchangeTokenOperation(
            KeyExchangeTokenOperation keyExchangeTokenOperation) {
        this.keyExchangeTokenOperation = keyExchangeTokenOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_KEYEXCHANGETOKEN,
                keyExchangeTokenOperation);
    }

    public void setRenewOperation(RenewOperation renewOperation) {
        this.renewOperation = renewOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_RENEW, renewOperation);
    }

    public void setRequestCollectionOperation(
            RequestCollectionOperation requestCollectionOperation) {
        this.requestCollectionOperation = requestCollectionOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_REQUESTCOLLECTION,
                requestCollectionOperation);
    }

    public void setValidateOperation(ValidateOperation validateOperation) {
        this.validateOperation = validateOperation;
        operationMap.put(WSTRUST_REQUESTTYPE_VALIDATE, validateOperation);
    }

    

    public Source invoke(Source request) {
        Source response = null;
        try {
            Object obj = convertToJAXBObject(request);
            Object operationImpl = null;
            Method method = null;
            if (obj instanceof RequestSecurityTokenCollectionType) {
                operationImpl = operationMap.get(WSTRUST_REQUESTTYPE_REQUESTCOLLECTION);
                method = OPERATION_METHODS.get(WSTRUST_REQUESTTYPE_REQUESTCOLLECTION);
            } else {
                RequestSecurityTokenType rst = (RequestSecurityTokenType)obj;
                List<?> objectList = rst.getAny();
                for (Object o : objectList) {
                    if (o instanceof JAXBElement) {
                        QName qname = ((JAXBElement<?>) o).getName();
                        if (qname.equals(new QName(WSTRUST_13_NAMESPACE,
                                WSTRUST_REQUESTTYPE_ELEMENTNAME))) {
                            String val = ((JAXBElement<?>) o).getValue().toString();
                            operationImpl = operationMap.get(val);
                            method =  OPERATION_METHODS.get(val);
                            break;
                        }
                    }
                }
            }

            if (operationImpl == null || method == null) {
                throw new Exception(
                        "Implementation for this operation not found.");
            }
            obj = method.invoke(operationImpl, obj, context);
            if (obj == null) {
                throw new Exception("Error in implementation class.");
            }
            if (obj instanceof RequestSecurityTokenResponseCollectionType) {
                RequestSecurityTokenResponseCollectionType tokenResponse =
                    (RequestSecurityTokenResponseCollectionType)obj;
                response = new JAXBSource(jaxbContext, 
                                          new ObjectFactory()
                                          .createRequestSecurityTokenResponseCollection(tokenResponse));
            } else {
                RequestSecurityTokenResponseType tokenResponse = 
                    (RequestSecurityTokenResponseType)obj;
                response = new JAXBSource(jaxbContext, 
                                          new ObjectFactory()
                                          .createRequestSecurityTokenResponse(tokenResponse));
            }

        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            throw createSOAPFault(cause);
        } catch (Exception ex) {
            throw createSOAPFault(ex);
        }

        return response;
    }
    
    private SoapFault createSOAPFault(Throwable ex) {
        String faultString = "Internal STS error";
        QName faultCode = null;
        
        if (ex != null) {
            if (ex instanceof STSException && ((STSException)ex).getFaultCode() != null) {
                faultCode = ((STSException)ex).getFaultCode();
            }
            faultString = ex.getMessage();
        }
        
        MessageContext messageContext = context.getMessageContext();
        SoapVersion soapVersion = (SoapVersion)messageContext.get(SoapVersion.class.getName());
        SoapFault fault;
        if (soapVersion.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(faultString, faultCode);
        } else {
            fault = new SoapFault(faultString, soapVersion.getSender());
            if (soapVersion.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }

    private Object convertToJAXBObject(Source source) throws Exception {
        //this is entirely to work around http://java.net/jira/browse/JAXB-909
        //if that bug is ever fixed and we can detect it, we can remove this 
        //complete and total HACK HACK HACK and replace with just:  
        //Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        //JAXBElement<?> jaxbElement = (JAXBElement<?>) unmarshaller.unmarshal(source);
        //return jaxbElement.getValue();
        
        Document d = StaxUtils.read(source);
        Binder<Node> binder = jaxbContext.createBinder();
        JAXBElement<?> jaxbElement = (JAXBElement<?>)binder.unmarshal(d);
        walkDom("", d.getDocumentElement(), binder, null);
        return jaxbElement.getValue();
    }

    private void walkDom(String pfx, Element element, Binder<Node> binder, Object parent) {
        try {
            Object o = binder.getJAXBNode(element);
            if (o instanceof JAXBElement) {
                o = ((JAXBElement<?>)o).getValue();
            }
            //System.out.println(pfx + DOMUtils.getElementQName(element) + " ->  " 
            //    + (o == null ? "null" : o.getClass()));
            if (o == null && parent != null) {
                // if it's not able to bind to an object, it's possibly an xsd:any
                // we'll check the parent for the standard "any" and replace with 
                // the original element.
                Field f = parent.getClass().getDeclaredField("any");
                if (f.getAnnotation(XmlAnyElement.class) != null) {
                    Object old = ReflectionUtil.setAccessible(f).get(parent);
                    if (old instanceof Element
                        && DOMUtils.getElementQName(element).equals(DOMUtils.getElementQName((Element)old))) {
                        ReflectionUtil.setAccessible(f).set(parent, element);
                    }
                }
            }
            if (o == null) {
                return;
            }
            Node nd = element.getFirstChild();
            while (nd != null) {
                if (nd instanceof Element) {
                    walkDom(pfx + "  ", (Element)nd, binder, o);
                }
                nd = nd.getNextSibling();
            }
        } catch (Throwable t) {
            //ignore -this is a complete hack anyway
        }
    }

    public CancelOperation getCancelOperation() {
        return cancelOperation;
    }

    public IssueOperation getIssueOperation() {
        return issueOperation;
    }
    
    public IssueSingleOperation getIssueSingleOperation() {
        return issueSingleOperation;
    }

    public KeyExchangeTokenOperation getKeyExchangeTokenOperation() {
        return keyExchangeTokenOperation;
    }

    public RenewOperation getRenewOperation() {
        return renewOperation;
    }

    public RequestCollectionOperation getRequestCollectionOperation() {
        return requestCollectionOperation;
    }

    public ValidateOperation getValidateOperation() {
        return validateOperation;
    }

}