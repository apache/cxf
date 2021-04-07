package org.apache.cxf.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentSchemaValidationHack;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.ServiceInfo;
import org.w3c.dom.Document;

/**
 * Provides support for reusing databinding context between multiple CXF
 * clients/servers. This is useful for fat WSDLs that produce multiple
 * interfaces and share classes between them.
 */
public class CachedJAXBDataBinding extends JAXBDataBinding {
    private static final Logger LOG = LogUtils.getLogger(CachedJAXBDataBinding.class);
    private JAXBContextCache.CachedContextAndSchemas cachedContextAndSchemas;
    public CachedJAXBDataBinding() {
        super();
    }

    public CachedJAXBDataBinding(boolean q) {
        super(q);
    }

    public CachedJAXBDataBinding(Class<?>... classes) throws JAXBException {
        super(classes);
    }

    public CachedJAXBDataBinding(boolean qualified, Map<String, Object> props) throws JAXBException {
        super(qualified, props);
    }

    public CachedJAXBDataBinding(JAXBContext context) {
        super(context);
    }

    @Override
    public synchronized void initialize(Service service) {
        inInterceptors.addIfAbsent(JAXBAttachmentSchemaValidationHack.INSTANCE);
        inFaultInterceptors.addIfAbsent(JAXBAttachmentSchemaValidationHack.INSTANCE);
        String tns = getNamespaceToUse(service);

        if(contextClasses == null) {
            contextClasses = new LinkedHashSet<>();
        }

        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            JAXBContextInitializer initializer = new JAXBContextInitializer(getBus(), serviceInfo, contextClasses,
                    typeRefs, this.getUnmarshallerProperties());
            initializer.walk();
            if (serviceInfo.getProperty("extra.class") != null) {
                Set<Class<?>> exClasses = serviceInfo.getProperty("extra.class", Set.class);
                contextClasses.addAll(exClasses);
            }

        }
        if (context == null) {
            JAXBContext ctx = null;
            try {
                cachedContextAndSchemas = createJAXBContextAndSchemas(contextClasses, tns);
            } catch (JAXBException e1) {
                throw new ServiceConstructionException(e1);
            }
            ctx = cachedContextAndSchemas.getContext();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "CREATED_JAXB_CONTEXT", new Object[]{ctx, contextClasses});
            }
            setContext(ctx);
        }


        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection col = serviceInfo.getXmlSchemaCollection();

            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                justCheckForJAXBAnnotations(serviceInfo);
                continue;
            }

            boolean schemasFromCache = false;
            Collection<DOMSource> schemas = getSchemas();
            if (schemas == null || schemas.isEmpty()) {
                schemas = cachedContextAndSchemas.getSchemas();
                if (schemas != null) {
                    schemasFromCache = true;
                }
            } else {
                schemasFromCache = true;
            }
            Set<DOMSource> bi = new LinkedHashSet<>();
            if (schemas == null) {
                schemas = new LinkedHashSet<>();
                try {
                    for (DOMResult r : generateJaxbSchemas()) {
                        DOMSource src = new DOMSource(r.getNode(), r.getSystemId());
                        if (BUILT_IN_SCHEMAS.containsValue(r)) {
                            bi.add(src);
                        } else {
                            schemas.add(src);
                        }
                    }
                    //put any builtins at the end.   Anything that DOES import them
                    //will cause it to load automatically and we'll skip them later
                    schemas.addAll(bi);
                } catch (IOException e) {
                    throw new ServiceConstructionException("SCHEMA_GEN_EXC", LOG, e);
                }
            }
            for (DOMSource r : schemas) {
                if (bi.contains(r)) {
                    String ns = ((Document)r.getNode()).getDocumentElement().getAttribute("targetNamespace");
                    if (serviceInfo.getSchema(ns) != null) {
                        continue;
                    }
                }
                //StaxUtils.print(r.getNode());
                //System.out.println();
                addSchemaDocument(serviceInfo,
                        col,
                        (Document)r.getNode(),
                        r.getSystemId());
            }

            JAXBSchemaInitializer schemaInit = new JAXBSchemaInitializer(serviceInfo, col, context, getQualifiedSchemas(), tns);
            schemaInit.walk();
            if (!schemasFromCache) {
                cachedContextAndSchemas.setSchemas(schemas);
            }
        }
    }

}
