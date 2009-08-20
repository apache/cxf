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

package org.apache.cxf.bus.spring;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.EncodedResource;

/**
 * CXF reads a series of Spring XML files as part of initialization.
 * The time it takes to parse them, especially if validating, builds up.
 * The XML files shipped in a release in the JARs are valid and invariant.
 * To speed things up, this class implements two levels of optimization.
 * When a CXF distribution is fully-packaged, each of the Spring XML 
 * bus extension .xml files is accompanied by a FastInfoset '.fixml' file.
 * These read much more rapidly. When one of those is present, this classs
 * reads it instead of reading the XML text file. 
 * 
 * Absent a .fixml file, this class uses WoodStox instead of Xerces (or
 * whatever the JDK is providing).
 * 
 * The Woodstox optimization also applies to user cxf.xml or cxf-servlet.xml files
 * if the user has disabled XML validation of Spring files with
 * the org.apache.cxf.spring.validation.mode system property.
 * 
 * Note that the fastInfoset optimization is only applied for the 
 * methods here that start from a Resource. If this is called with an InputSource,
 * that optimization is not applied, since we can't reliably know the
 * location of the XML. 
 */
public class ControlledValidationXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    /**
     * Exception class used to avoid reading old FastInfoset files.
     */
    private static class StaleFastinfosetException extends Exception {

    }

    // the following flag allows performance comparisons with and 
    // without fast infoset processing.
    private boolean noFastinfoset;
    // Spring has no 'getter' for this, so we need our own copy.
    private int visibleValidationMode = VALIDATION_AUTO;
    // We need a reference to the subclass.
    private TunedDocumentLoader tunedDocumentLoader;
    /**
     * @param beanFactory
     */
    public ControlledValidationXmlBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
        super(beanFactory);
        tunedDocumentLoader = new TunedDocumentLoader();
        this.setDocumentLoader(tunedDocumentLoader);
        noFastinfoset = System.getProperty("org.apache.cxf.nofastinfoset") != null 
            || !TunedDocumentLoader.hasFastInfoSet();
    }

    @Override
    protected int doLoadBeanDefinitions(InputSource inputSource, 
                                        Resource resource) throws BeanDefinitionStoreException {
        // sadly, the Spring class we are extending has the critical function
        // getValidationModeForResource
        // marked private instead of protected, so trickery is called for here.
        boolean suppressValidation = false;
        try {
            URL url = resource.getURL();
            if (url.getFile().contains("META-INF/cxf/")) {
                suppressValidation = true;
            }
        } catch (IOException e) {
            // this space intentionally left blank.
        }
        
        int savedValidation = visibleValidationMode;
        if (suppressValidation) {
            setValidationMode(VALIDATION_NONE);
        }
        int r = super.doLoadBeanDefinitions(inputSource, resource);
        setValidationMode(savedValidation);
        return r;
    }

    @Override
    public void setValidationMode(int validationMode) {
        visibleValidationMode = validationMode;
        super.setValidationMode(validationMode);
    }

    @Override
    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        if (!noFastinfoset) {
            try {
                return fastInfosetLoadBeanDefinitions(encodedResource);
            } catch (BeanDefinitionStoreException bdse) {
                throw bdse;
            } catch (Throwable e) {
                //ignore - just call the super to load them
            }
        }
        return super.loadBeanDefinitions(encodedResource);
    }
    
    private int fastInfosetLoadBeanDefinitions(EncodedResource encodedResource)
        throws IOException, StaleFastinfosetException, 
        ParserConfigurationException, XMLStreamException {
        
        URL resUrl = encodedResource.getResource().getURL();
        // There are XML files scampering around that don't end in .xml.
        // We don't apply the optimization to them.
        if (!resUrl.getPath().endsWith(".xml")) {
            throw new StaleFastinfosetException();
        }
        String fixmlPath = resUrl.getPath().replaceFirst("\\.xml$", ".fixml");
        String protocol = resUrl.getProtocol();
        // beware of the relative URL rules for jar:, which are surprising.
        if ("jar".equals(protocol)) {
            fixmlPath = fixmlPath.replaceFirst("^.*!", "");
        }
        
        URL fixmlUrl = new URL(resUrl, fixmlPath);

        // if we are in unpacked files, we take some extra time
        // to ensure that we aren't using a stale Fastinfoset file.
        if ("file".equals(protocol)) {
            URLConnection resCon = null;
            URLConnection fixCon = null;
            resCon = resUrl.openConnection();
            fixCon = fixmlUrl.openConnection();
            if (resCon.getLastModified() > fixCon.getLastModified()) {
                throw new StaleFastinfosetException();
            }
        }
        
        Resource newResource = new UrlResource(fixmlUrl); 
        Document doc = TunedDocumentLoader.loadFastinfosetDocument(fixmlUrl);
        if (doc == null) {
            //something caused FastinfoSet to not be able to read the doc
            throw new StaleFastinfosetException();
        }
        return registerBeanDefinitions(doc, newResource);
    }

}
