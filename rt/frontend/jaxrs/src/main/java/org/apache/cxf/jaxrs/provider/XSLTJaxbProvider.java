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
package org.apache.cxf.jaxrs.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

@Produces({"application/xml", "application/*+xml", "text/xml", "text/html" })
@Consumes({"application/xml", "application/*+xml", "text/xml", "text/html" })
@Provider
public class XSLTJaxbProvider extends JAXBElementProvider {
    
    private static final Logger LOG = LogUtils.getL7dLogger(XSLTJaxbProvider.class);
    
    private static final String ABSOLUTE_PATH_PARAMETER = "absolute.path";
    private static final String BASE_PATH_PARAMETER = "base.path";
    private static final String RELATIVE_PATH_PARAMETER = "relative.path";
    
    private SAXTransformerFactory factory;
    private Templates inTemplates;
    private Templates outTemplates;
    private Map<String, Templates> inMediaTemplates;
    private Map<String, Templates> outMediaTemplates;

    private List<String> inClassesToHandle;
    private List<String> outClassesToHandle;
    private Map<String, Object> inParamsMap;
    private Map<String, Object> outParamsMap;
    private Map<String, String> inProperties;
    private Map<String, String> outProperties;
    private URIResolver uriResolver;
    private String systemId;
    
    private boolean supportJaxbOnly;
    
    public void setSupportJaxbOnly(boolean support) {
        this.supportJaxbOnly = support;
    }
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        // JAXB support is required
        if (!super.isReadable(type, genericType, anns, mt)) {
            return false;
        }
        // if the user has set the list of in classes and a given class 
        // is in that list then it can only be handled by the template
        if (inClassCanBeHandled(type.getName())) {
            return inTemplatesAvailable(mt); 
        } else {
            return supportJaxbOnly;
        }
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        // JAXB support is required
        if (!super.isReadable(type, genericType, anns, mt)) {
            return false;
        }
        // if the user has set the list of out classes and a given class 
        // is in that list then it can only be handled by the template
        if (outClassCanBeHandled(type.getName())) {
            return outTemplatesAvailable(mt); 
        } else {
            return supportJaxbOnly;
        }
    }
    
    protected boolean inTemplatesAvailable(MediaType mt) {
        return inTemplates != null 
            || inMediaTemplates != null && inMediaTemplates.containsKey(mt.getType() + "/" 
                                                                        + mt.getSubtype());
    }
    
    protected boolean outTemplatesAvailable(MediaType mt) {
        return outTemplates != null 
            || outMediaTemplates != null && outMediaTemplates.containsKey(mt.getType() 
                                                                          + "/" + mt.getSubtype());
    }
    
    protected Templates getInTemplates(MediaType mt) {
        return inTemplates != null ? inTemplates 
            : inMediaTemplates != null ? inMediaTemplates.get(mt.getType() + "/" + mt.getSubtype()) : null;
    }
    
    protected Templates getOutTemplates(MediaType mt) {
        return outTemplates != null ? outTemplates 
            : outMediaTemplates != null ? outMediaTemplates.get(mt.getType() + "/" + mt.getSubtype()) : null;
    }
    
    @Override
    protected Object unmarshalFromInputStream(Unmarshaller unmarshaller, InputStream is, MediaType mt) 
        throws JAXBException {
        try {

            Templates t = createTemplates(getInTemplates(mt), inParamsMap, inProperties);
            if (t == null && supportJaxbOnly) {
                return super.unmarshalFromInputStream(unmarshaller, is, mt);
            }
            XMLFilter filter = factory.newXMLFilter(t);
            SAXSource source = new SAXSource(filter, new InputSource(is));
            if (systemId != null) {
                source.setSystemId(systemId);
            }
            return unmarshaller.unmarshal(source);
        } catch (TransformerConfigurationException ex) {
            LOG.warning("Transformation exception : " + ex.getMessage());
            throw new WebApplicationException(ex);
        }
    }
    
    protected Object unmarshalFromReader(Unmarshaller unmarshaller, XMLStreamReader reader, MediaType mt) 
        throws JAXBException {
        CachedOutputStream out = new CachedOutputStream();
        try {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
            StaxUtils.copy(new StaxSource(reader), writer);
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            return unmarshalFromInputStream(unmarshaller, out.getInputStream(), mt);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    @Override
    protected void marshalToWriter(Marshaller ms, Object obj, XMLStreamWriter writer, MediaType mt) 
        throws Exception {
        CachedOutputStream out = new CachedOutputStream();
        marshalToOutputStream(ms, obj, out, mt);
        
        StaxUtils.copy(new StreamSource(out.getInputStream()), writer);
    }
    
    @Override
    protected void addAttachmentMarshaller(Marshaller ms) {
        // complete
    }
    
    @Override
    protected void marshalToOutputStream(Marshaller ms, Object obj, OutputStream os, MediaType mt)
        throws Exception {
        
        Templates t = createTemplates(getOutTemplates(mt), outParamsMap, outProperties);
        if (t == null && supportJaxbOnly) {
            super.marshalToOutputStream(ms, obj, os, mt);
            return;
        }
        
        TransformerHandler th = factory.newTransformerHandler(t);
        Result result = new StreamResult(os);
        if (systemId != null) {
            result.setSystemId(systemId);
        }
        th.setResult(result);
        
        if (getContext() == null) {
            th.startDocument();
        }
        ms.marshal(obj, th);
        if (getContext() == null) {
            th.endDocument();
        }
    }
    
    public void setOutTemplate(String loc) {
        outTemplates = createTemplates(loc);
    }
    
    public void setInTemplate(String loc) {
        inTemplates = createTemplates(loc);
    }
    
    public void setInMediaTemplates(Map<String, String> map) {
        inMediaTemplates = new HashMap<String, Templates>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            inMediaTemplates.put(entry.getKey(), createTemplates(entry.getValue()));
        }
    }
    
    public void setOutMediaTemplates(Map<String, String> map) {
        outMediaTemplates = new HashMap<String, Templates>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            outMediaTemplates.put(entry.getKey(), createTemplates(entry.getValue()));
        }
    }
    
    public void setResolver(URIResolver resolver) {
        uriResolver = resolver;
        if (factory != null) {
            factory.setURIResolver(uriResolver);
        }
    }
    
    public void setSystemId(String system) {
        systemId = system;
    }
    
    public void setInParameters(Map<String, Object> inParams) {
        this.inParamsMap = inParams;
    }
    
    public void setOutParameters(Map<String, Object> outParams) {
        this.outParamsMap = outParams;
    }
    
    public void setInProperties(Map<String, String> inProps) {
        this.inProperties = inProps;
    }
    
    public void setOutProperties(Map<String, String> outProps) {
        this.outProperties = outProps;
    }
    
    public void setInClassNames(List<String> classNames) {
        inClassesToHandle = classNames;
    }
    
    public boolean inClassCanBeHandled(String className) {
        return inClassesToHandle == null || inClassesToHandle.contains(className); 
    }
    
    public void setOutClassNames(List<String> classNames) {
        outClassesToHandle = classNames;
    }
    
    public boolean outClassCanBeHandled(String className) {
        return outClassesToHandle == null || outClassesToHandle.contains(className); 
    }
    
    protected Templates createTemplates(Templates templates, 
                                      Map<String, Object> configuredParams,
                                      Map<String, String> outProps) {
        if (templates == null) {
            if (supportJaxbOnly) {
                return null;
            } else {
                LOG.severe("No template is available");
                throw new WebApplicationException(500);
            }
        }
        
        TemplatesImpl templ =  new TemplatesImpl(templates, uriResolver);
        MessageContext mc = getContext();
        if (mc != null) {
            UriInfo ui = mc.getUriInfo();
            MultivaluedMap<String, String> params = ui.getPathParameters();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String value = entry.getValue().get(0);
                int ind = value.indexOf(";");
                if (ind > 0) {
                    value = value.substring(0, ind);
                }
                templ.setTransformerParameter(entry.getKey(), value);
            }
            
            List<PathSegment> segments = ui.getPathSegments();
            if (segments.size() > 0) {
                setTransformParameters(templ, segments.get(segments.size() - 1).getMatrixParameters());
            }
            setTransformParameters(templ, ui.getQueryParameters());
            templ.setTransformerParameter(ABSOLUTE_PATH_PARAMETER, ui.getAbsolutePath().toString());
            templ.setTransformerParameter(RELATIVE_PATH_PARAMETER, ui.getPath());
            templ.setTransformerParameter(BASE_PATH_PARAMETER, ui.getBaseUri().toString());
            if (configuredParams != null) {
                for (Map.Entry<String, Object> entry : configuredParams.entrySet()) {
                    templ.setTransformerParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        if (outProps != null) {
            templ.setOutProperties(outProps);
        }
        
        return templ;
    }

    private void setTransformParameters(TemplatesImpl templ, MultivaluedMap<String, String> params) {
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            templ.setTransformerParameter(entry.getKey(), entry.getValue().get(0));
        }    
    }
    
    protected Templates createTemplates(String loc) {
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, this.getBus());
            if (is == null) {
                return null;
            }
            
            Reader r = new BufferedReader(
                           new InputStreamReader(is, "UTF-8"));
            Source source = new StreamSource(r);
            if (factory == null) {
                factory = (SAXTransformerFactory)TransformerFactory.newInstance();
                if (uriResolver != null) {
                    factory.setURIResolver(uriResolver);
                }
            }
            return factory.newTemplates(source);
            
        } catch (Exception ex) {
            LOG.warning("No template can be created : " + ex.getMessage());
        }
        return null;
    }
    
    private static class TemplatesImpl implements Templates {

        private Templates templates;
        private URIResolver resolver;
        private Map<String, Object> transformParameters = new HashMap<String, Object>();
        private Map<String, String> outProps = new HashMap<String, String>();
        
        public TemplatesImpl(Templates templates, URIResolver resolver) {
            this.templates = templates;
            this.resolver = resolver;
        }
        
        public void setTransformerParameter(String name, Object value) {
            transformParameters.put(name, value);
        }
        
        public void setOutProperties(Map<String, String> props) {
            this.outProps = props;
        }
        
        public Properties getOutputProperties() {
            return templates.getOutputProperties();
        }

        public Transformer newTransformer() throws TransformerConfigurationException {
            Transformer tr = templates.newTransformer();
            tr.setURIResolver(resolver);
            for (Map.Entry<String, Object> entry : transformParameters.entrySet()) {
                tr.setParameter(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : outProps.entrySet()) {
                tr.setOutputProperty(entry.getKey(), entry.getValue());
            }
            return tr;
        }
        
    }
}
