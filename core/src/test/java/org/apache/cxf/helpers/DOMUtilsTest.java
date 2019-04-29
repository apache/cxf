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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.junit.Test;

public class DOMUtilsTest {

    @Test
    public void getDomElement() throws ParserConfigurationException, IOException, SAXException {
        System.out.println("----------------------------------------------------------");
        readDocument(true);
        System.out.println("----------------------------------------------------------");
        readDocument(false);
        System.out.println("----------------------------------------------------------");
    }

    private void readDocument(boolean useOriginal) throws ParserConfigurationException, IOException, SAXException {
        String msg = " elapsed time";
        if (useOriginal) {
            msg += " with original DOMUtils | ";
        } else {
            msg += " with cached DOMUtils   | ";
        }

        File fXmlFile = new File("src/test/resources/xml/large.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        long startTime = System.nanoTime();
        recursiveReadNode(doc.getChildNodes(), useOriginal);
        long estimatedTime = System.nanoTime() - startTime;

        System.out.println(msg + convertNanoToMillis(estimatedTime) + " milliseconds");
    }

    /**
     * domUtilsType
     * 0) no call to DOMUtils
     * 1) call DOMUtils without caching
     * 2) call DOMUtils with caching
     */
    private void recursiveReadNode(NodeList nodeList, boolean useOriginal) {
        int nodeLength = nodeList.getLength();
        for (int index = 0; index < nodeLength; index++) {
            Node node = nodeList.item(index);

            if (useOriginal) {
                getDomElement(node);
            } else {
                DOMUtils.getDomElement(node);
            }

            if (node.hasChildNodes()) {
                recursiveReadNode(node.getChildNodes(), useOriginal);
            }
        }
    }

    private double convertNanoToMillis(long nanotime) {
        long micro = TimeUnit.NANOSECONDS.toMicros(nanotime);
        return (double) micro / 1_000;
    }


    // -----------------------------------------------------------------------------
    // ------------- original DOMUtils.getDomElement -------------------------------
    // -----------------------------------------------------------------------------

    /**
     * Try to get the DOM Node from the SAAJ Node with JAVA9 afterwards
     *
     * @param node The original node we need check
     * @return The DOM node
     */
    public static Node getDomElement(Node node) {
        if (node != null) {
            //java9plus hack
            try {
                Method method = node.getClass().getMethod("getDomElement");
                node = (Node) method.invoke(node);
            } catch (NoSuchMethodException e) {
                //best effort to try, do nothing if NoSuchMethodException
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }
}