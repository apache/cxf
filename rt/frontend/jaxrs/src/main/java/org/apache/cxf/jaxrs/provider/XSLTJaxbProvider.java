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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
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
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.xml.XSLTTransform;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

@Produces({"application/xml", "application/*+xml", "text/xml", "text/html" })
@Consumes({"application/xml", "application/*+xml", "text/xml", "text/html" })
@Provider
public class XSLTJaxbProvider<T> extends JAXBElementProvider<T> {

    private static final Logger LOG = LogUtils.getL7dLogger(XSLTJaxbProvider.class);

    private static final String ABSOLUTE_PATH_PARAMETER = "absolute.path";
    private static final String BASE_PATH_PARAMETER = "base.path";
    private static final String RELATIVE_PATH_PARAMETER = "relative.path";
    private static final String XSLT_TEMPLATE_PROPERTY = "xslt.template";
    private SAXTransformerFactory factory;
    private Templates inTemplates;
    private Templates outTemplates;
    private Map<String, Templates> inMediaTemplates;
    private Map<String, Templates> outMediaTemplates;
    private ConcurrentHashMap<String, Templates> annotationTemplates =
        new ConcurrentHashMap<>();

    private List<String> inClassesToHandle;
    private List<String> outClassesToHandle;
    private Map<String, Object> inParamsMap;
    private Map<String, Object> outParamsMap;
    private Map<String, String> inProperties;
    private Map<String, String> outProperties;
    private URIResolver uriResolver;
    private String systemId;

    private boolean supportJaxbOnly;
    private boolean refreshTemplates;
    private boolean secureProcessing = true;

    public void setSupportJaxbOnly(boolean support) {
        this.supportJaxbOnly = support;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        if (!super.isReadable(type, genericType, anns, mt)) {
            return false;
        }

        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            return supportJaxbOnly;
        }

        // if the user has set the list of in classes and a given class
        // is in that list then it can only be handled by the template
        if (inClassCanBeHandled(type.getName()) || inClassesToHandle == null && !supportJaxbOnly) {
            return inTemplatesAvailable(type, anns, mt);
        }
        return supportJaxbOnly;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        // JAXB support is required
        if (!super.isWriteable(type, genericType, anns, mt)) {
            return false;
        }
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            return supportJaxbOnly;
        }

        // if the user has set the list of out classes and a given class
        // is in that list then it can only be handled by the template
        if (outClassCanBeHandled(type.getName()) || outClassesToHandle == null && !supportJaxbOnly) {
            return outTemplatesAvailable(type, anns, mt);
        }
        return supportJaxbOnly;
    }

    protected boolean inTemplatesAvailable(Class<?> cls, Annotation[] anns, MediaType mt) {
        return inTemplates != null
            || inMediaTemplates != null && inMediaTemplates.containsKey(mt.getType() + "/"
                                                                        + mt.getSubtype())
            || getTemplatesFromAnnotation(cls, anns, mt) != null;
    }

    protected boolean outTemplatesAvailable(Class<?> cls, Annotation[] anns, MediaType mt) {
        return outTemplates != null
            || outMediaTemplates != null && outMediaTemplates.containsKey(mt.getType()
                                                                          + "/" + mt.getSubtype())
            || getTemplatesFromAnnotation(cls, anns, mt) != null;
    }

    protected Templates getTemplatesFromAnnotation(Class<?> cls,
                                                   Annotation[] anns,
                                                   MediaType mt) {
        Templates t = null;
        XSLTTransform ann = getXsltTransformAnn(anns, mt);
        if (ann != null) {
            t = annotationTemplates.get(ann.value());
            if (t == null || refreshTemplates) {
                String path = ann.value();
                final String cp = "classpath:";
                if (!path.startsWith(cp)) {
                    path = cp + path;
                }
                t = createTemplates(path);
                if (t == null) {
                    createTemplates(ClassLoaderUtils.getResource(ann.value(), cls));
                }
                if (t != null) {
                    annotationTemplates.put(ann.value(), t);
                }
            }
        }
        return t;

    }

    protected Templates getAnnotationTemplates(Annotation[] anns) {
        Templates t = null;
        XSLTTransform ann = AnnotationUtils.getAnnotation(anns, XSLTTransform.class);
        if (ann != null) {
            t = annotationTemplates.get(ann.value());
        }
        return t;

    }

    protected XSLTTransform getXsltTransformAnn(Annotation[] anns, MediaType mt) {
        XSLTTransform ann = AnnotationUtils.getAnnotation(anns, XSLTTransform.class);
        if (ann != null && ann.type() != XSLTTransform.TransformType.CLIENT) {
            if (ann.mediaTypes().length > 0) {
                for (String s : ann.mediaTypes()) {
                    if (mt.isCompatible(JAXRSUtils.toMediaType(s))) {
                        return ann;
                    }
                }
                return null;
            }
            return ann;
        }
        return null;
    }



    protected Templates getInTemplates(Annotation[] anns, MediaType mt) {
        Templates t = createTemplatesFromContext();
        if (t != null) {
            return t;
        }
        t = inTemplates != null ? inTemplates
            : inMediaTemplates != null ? inMediaTemplates.get(mt.getType() + "/" + mt.getSubtype()) : null;
        if (t == null) {
            t = getAnnotationTemplates(anns);
        }
        return t;
    }

    protected Templates getOutTemplates(Annotation[] anns, MediaType mt) {
        Templates t = createTemplatesFromContext();
        if (t != null) {
            return t;
        }
        t = outTemplates != null ? outTemplates
            : outMediaTemplates != null ? outMediaTemplates.get(mt.getType() + "/" + mt.getSubtype()) : null;
        if (t == null) {
            t = getAnnotationTemplates(anns);
        }
        return t;
    }

    @Override
    protected Object unmarshalFromInputStream(Unmarshaller unmarshaller, InputStream is,
                                              Annotation[] anns, MediaType mt)
        throws JAXBException {
        try {

            Templates t = createTemplates(getInTemplates(anns, mt), inParamsMap, inProperties);
            if (t == null && supportJaxbOnly) {
                return super.unmarshalFromInputStream(unmarshaller, is, anns, mt);
            }

            if (unmarshaller.getClass().getName().contains("eclipse")) {
                //eclipse MOXy doesn't work properly with the XMLFilter/Reader thing
                //so we need to bounce through a DOM
                Source reader = new StaxSource(StaxUtils.createXMLStreamReader(is));
                DOMResult dom = new DOMResult();
                t.newTransformer().transform(reader, dom);
                return unmarshaller.unmarshal(dom.getNode());
            }
            XMLFilter filter;
            try {
                filter = factory.newXMLFilter(t);
            } catch (TransformerConfigurationException ex) {
                TemplatesImpl ti = (TemplatesImpl)t;
                filter = factory.newXMLFilter(ti.getTemplates());
                trySettingProperties(filter, ti);
            }
            XMLReader reader = new StaxSource(StaxUtils.createXMLStreamReader(is));
            filter.setParent(reader);
            SAXSource source = new SAXSource();
            source.setXMLReader(filter);
            if (systemId != null) {
                source.setSystemId(systemId);
            }
            return unmarshaller.unmarshal(source);
        } catch (TransformerException ex) {
            LOG.warning("Transformation exception : " + ex.getMessage());
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    private void trySettingProperties(Object filter, TemplatesImpl ti) {
        try {
            //Saxon doesn't allow creating a Filter or Handler from anything other than it's original
            //Templates.  That then requires setting the parameters after the fact, but there
            //isn't a standard API for that, so we have to grab the Transformer via reflection to
            //set the parameters.
            Transformer tr = (Transformer)filter.getClass().getMethod("getTransformer").invoke(filter);
            tr.setURIResolver(ti.resolver);
            for (Map.Entry<String, Object> entry : ti.transformParameters.entrySet()) {
                tr.setParameter(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : ti.outProps.entrySet()) {
                tr.setOutputProperty(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not set properties for transfomer", e);
        }
    }

    @Override
    protected Object unmarshalFromReader(Unmarshaller unmarshaller, XMLStreamReader reader,
                                         Annotation[] anns, MediaType mt)
        throws JAXBException {
        CachedOutputStream out = new CachedOutputStream();
        try {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
            StaxUtils.copy(new StaxSource(reader), writer);
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            return unmarshalFromInputStream(unmarshaller, out.getInputStream(), anns, mt);
        } catch (Exception ex) {
            throw ExceptionUtils.toBadRequestException(ex, null);
        }
    }

    @Override
    protected void marshalToWriter(Marshaller ms, Object obj, XMLStreamWriter writer,
                                   Annotation[] anns, MediaType mt)
        throws Exception {
        CachedOutputStream out = new CachedOutputStream();
        marshalToOutputStream(ms, obj, out, anns, mt);

        StaxUtils.copy(new StreamSource(out.getInputStream()), writer);
    }

    @Override
    protected void addAttachmentMarshaller(Marshaller ms) {
        // complete
    }

    protected Result getStreamResult(OutputStream os, Annotation[] anns, MediaType mt) throws Exception {
        return new StreamResult(os);
    }

    @Override
    protected void marshalToOutputStream(Marshaller ms, Object obj, OutputStream os,
                                         Annotation[] anns, MediaType mt)
        throws Exception {

        Templates t = createTemplates(getOutTemplates(anns, mt), outParamsMap, outProperties);
        if (t == null && supportJaxbOnly) {
            super.marshalToOutputStream(ms, obj, os, anns, mt);
            return;
        }
        org.apache.cxf.common.jaxb.JAXBUtils.setMinimumEscapeHandler(ms);
        TransformerHandler th;
        try {
            th = factory.newTransformerHandler(t);
        } catch (TransformerConfigurationException ex) {
            TemplatesImpl ti = (TemplatesImpl)t;
            th = factory.newTransformerHandler(ti.getTemplates());
            this.trySettingProperties(th, ti);
        }
        Result result = getStreamResult(os, anns, mt);
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
        inMediaTemplates = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            inMediaTemplates.put(entry.getKey(), createTemplates(entry.getValue()));
        }
    }

    public void setOutMediaTemplates(Map<String, String> map) {
        outMediaTemplates = new HashMap<>();
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
        return inClassesToHandle != null && inClassesToHandle.contains(className);
    }

    public void setOutClassNames(List<String> classNames) {
        outClassesToHandle = classNames;
    }

    public boolean outClassCanBeHandled(String className) {
        return outClassesToHandle != null && outClassesToHandle.contains(className);
    }

    protected Templates createTemplates(Templates templates,
                                      Map<String, Object> configuredParams,
                                      Map<String, String> outProps) {
        if (templates == null) {
            if (supportJaxbOnly) {
                return null;
            }
            LOG.severe("No template is available");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        TemplatesImpl templ = new TemplatesImpl(templates, uriResolver);
        MessageContext mc = getContext();
        if (mc != null) {
            UriInfo ui = mc.getUriInfo();
            MultivaluedMap<String, String> params = ui.getPathParameters();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String value = entry.getValue().get(0);
                int ind = value.indexOf(';');
                if (ind > 0) {
                    value = value.substring(0, ind);
                }
                templ.setTransformerParameter(entry.getKey(), value);
            }

            List<PathSegment> segments = ui.getPathSegments();
            if (!segments.isEmpty()) {
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
            return createTemplates(ResourceUtils.getResourceURL(loc, this.getBus()));
        } catch (Exception ex) {
            LOG.warning("No template can be created : " + ex.getMessage());
        }
        return null;
    }

    protected Templates createTemplatesFromContext() {
        MessageContext mc = getContext();
        if (mc != null) {
            String template = (String)mc.getContextualProperty(XSLT_TEMPLATE_PROPERTY);
            if (template != null) {
                return createTemplates(template);
            }
        }
        return null;
    }

    protected Templates createTemplates(URL urlStream) {
        if (urlStream == null) {
            return null;
        }

        try (Reader r = new BufferedReader(
                           new InputStreamReader(urlStream.openStream(), StandardCharsets.UTF_8))) {
            Source source = new StreamSource(r);
            source.setSystemId(urlStream.toExternalForm());
            if (factory == null) {
                factory = (SAXTransformerFactory)TransformerFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
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

    public void setRefreshTemplates(boolean refresh) {
        this.refreshTemplates = refresh;
    }

    public void setSecureProcessing(boolean secureProcessing) {
        this.secureProcessing = secureProcessing;
    }

    private static class TemplatesImpl implements Templates {

        private Templates templates;
        private URIResolver resolver;
        private Map<String, Object> transformParameters = new HashMap<>();
        private Map<String, String> outProps = new HashMap<>();

        TemplatesImpl(Templates templates, URIResolver resolver) {
            this.templates = templates;
            this.resolver = resolver;
        }

        public Templates getTemplates() {
            return templates;
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
