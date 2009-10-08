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

package org.apache.cxf.tools.common.toolspec.parser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.toolspec.Tool;
import org.apache.cxf.tools.common.toolspec.ToolSpec;

public class CommandLineParser {

    private static final Logger LOG = LogUtils.getL7dLogger(CommandLineParser.class);
    private ToolSpec toolspec;

    public CommandLineParser(ToolSpec ts) {
        this.toolspec = ts;
    }

    public void setToolSpec(ToolSpec ts) {
        this.toolspec = ts;
    }

    public static String[] getArgsFromString(String s) {
        StringTokenizer toker = new StringTokenizer(s);
        List<Object> res = new ArrayList<Object>();

        while (toker.hasMoreTokens()) {
            res.add(toker.nextToken());
        }
        return res.toArray(new String[res.size()]);
    }

    public CommandDocument parseArguments(String args) throws BadUsageException {
        return parseArguments(getArgsFromString(args));
    }

    public CommandDocument parseArguments(String[] args) throws BadUsageException {

        if (LOG.isLoggable(Level.FINE)) {
            StringBuffer debugMsg = new StringBuffer("Parsing arguments: ");

            for (int i = 0; i < args.length; i++) {
                debugMsg.append(args[i]).append(" ");
            }
            LOG.fine(debugMsg.toString());
        }

        if (toolspec == null) {
            throw new IllegalStateException("No schema known- call to acceptSc"
                                            + "hema() must be made and must succeed");
        }

        // Create a result document

        Document resultDoc = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            resultDoc = factory.newDocumentBuilder().newDocument();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "FAIL_CREATE_DOM_MSG");
        }
        Element commandEl = resultDoc.createElementNS("http://cxf.apache.org/Xutil/Command", "command");
        
        Attr attr = 
            commandEl.getOwnerDocument().createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", 
                                                                   "xsi:schemaLocation");
        attr.setValue("http://cxf.apache.org/Xutil/Command http://cxf.apache.org/schema/xutil/commnad.xsd");
        commandEl.setAttributeNodeNS(attr);     
        commandEl.setAttribute("xmlns", "http://cxf.apache.org/Xutil/Command");
        commandEl.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        resultDoc.appendChild(commandEl);

        TokenInputStream tokens = new TokenInputStream(args);

        // for all form elements...
        Element usage = toolspec.getUsage();

        List<Element> usageForms = toolspec.getUsageForms();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found " + usageForms.size()
                      + " alternative forms of usage, will use default form");
        }
        if (usageForms.size() > 0) {
            ErrorVisitor errors = new ErrorVisitor();
            
            for (Element elem : usageForms) {
                Form form = new Form(elem);

                int pos = tokens.getPosition();

                if (form.accept(tokens, commandEl, errors)) {
                    commandEl.setAttribute("form", form.getName());
                    break;
                } else {
                    // if no more left then return null;
                    tokens.setPosition(pos);
                    
                    if (elem.getNextSibling() == null) {
                        if (LOG.isLoggable(Level.INFO)) {
                            LOG.info("No more forms left to try, returning null");
                        }
                        throwUsage(errors);
                    }
                }
            
                
            }
/*
            for (int i = 0; i < usageForms.getLength(); i++) {
                Form form = new Form((Element)usageForms.item(i));

                int pos = tokens.getPosition();

                if (form.accept(tokens, commandEl, errors)) {
                    commandEl.setAttribute("form", form.getName());
                    break;
                } else {
                    // if no more left then return null;
                    tokens.setPosition(pos);
                    if (i == usageForms.getLength() - 1) {
                        if (LOG.isLoggable(Level.INFO)) {
                            LOG.info("No more forms left to try, returning null");
                        }
                        throwUsage(errors);
                    }
                }
            }
*/
        } else {
            ErrorVisitor errors = new ErrorVisitor();
            Form form = new Form(usage);

            if (!form.accept(tokens, commandEl, errors)) {
                throwUsage(errors);
            }
        }

        // output the result document
        if (LOG.isLoggable(Level.FINE)) {
            try {
                Transformer serializer = TransformerFactory.newInstance()
                    .newTransformer(
                                    new StreamSource(Tool.class
                                        .getResourceAsStream("indent-no-xml-declaration.xsl")));

                serializer.transform(new DOMSource(resultDoc), new StreamResult(new PrintStream(System.out)));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "ERROR_SERIALIZE_COMMAND_MSG", ex);
            }
        }

        return new CommandDocument(toolspec, resultDoc);
    }

    public void throwUsage(ErrorVisitor errors) throws BadUsageException {
        try {
            throw new BadUsageException(getUsage(), errors);
        } catch (TransformerException ex) {
            LOG.log(Level.SEVERE, "CANNOT_GET_USAGE_MSG", ex);
            throw new BadUsageException(errors);
        }
    }

    public String getUsage() throws TransformerException {
        // REVISIT: style usage document into a form more readily output as a
        // usage message
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = getClass().getResourceAsStream("usage.xsl");

        toolspec.transform(in, baos);
        return baos.toString();
    }

    public String getDetailedUsage() throws TransformerException {
        // REVISIT: style usage document into a form more readily output as a
        // usage message
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        toolspec.transform(getClass().getResourceAsStream("detailedUsage.xsl"), baos);
        return baos.toString();
    }

    public String getFormattedDetailedUsage() throws TransformerException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        toolspec.transform(getClass().getResourceAsStream("detailedUsage.xsl"), baos);
        String usage = baos.toString();
        // we use the following pattern to format usage
        // |-------|-options|------|description-----------------|
        // before option white space size is 7
        int beforeOptSpan = 3;
        // option length is 8
        int optSize = 12;
        // after option white space size is 6
        int afterOptLen = 6;
        int totalLen = 80;
        int optSpan = optSize + afterOptLen - 1;
        int beforeDesSpan = beforeOptSpan + optSpan + 1;
        String lineSeparator = System.getProperty("line.separator");
        StringTokenizer st1 = new StringTokenizer(usage, lineSeparator);
        int i = 0;
        int length = st1.countTokens();
        String[] originalStrs = new String[length];
        while (st1.hasMoreTokens()) {
            String str = st1.nextToken();
            originalStrs[i] = str;
            i++;
        }
        StringBuffer strbuffer = new StringBuffer();
        for (int j = 0; j < length - 1; j = j + 2) {
            int optionLen = originalStrs[j].length();
            addWhiteNamespace(strbuffer, beforeOptSpan);
            if (optionLen <= optSpan) {
                // && beforeOptSpan + optionLen + optSpan + desLen <= totalLen -
                // 1) {

                strbuffer.append(originalStrs[j]);
                addWhiteNamespace(strbuffer, optSpan - originalStrs[j].length());
                strbuffer.append(" ");
                if (originalStrs[j + 1].length() > totalLen - beforeDesSpan) {
                    int lastIdx = totalLen - beforeDesSpan; 
                    int lastIdx2 = splitAndAppendText(strbuffer, originalStrs[j + 1], 0, lastIdx);
                    originalStrs[j + 1] = originalStrs[j + 1].substring(lastIdx2);
                    strbuffer.append(lineSeparator);
                } else {
                    strbuffer.append(originalStrs[j + 1]);
                    strbuffer.append(lineSeparator);
                    originalStrs[j + 1] = "";
                }
            } else {
                strbuffer.append(originalStrs[j]);
                strbuffer.append(lineSeparator);
            }
            String tmpStr = originalStrs[j + 1];
            
            for (i = 0; i < tmpStr.length(); i = i + (totalLen - beforeDesSpan)) {
                if (i + totalLen - beforeDesSpan < tmpStr.length()) {
                    addWhiteNamespace(strbuffer, beforeDesSpan);
                    int lastIdx = i + totalLen - beforeDesSpan; 
                    int lastIdx2 = splitAndAppendText(strbuffer, tmpStr, i, lastIdx);
                    i += lastIdx2 - lastIdx; 
                    strbuffer.append(lineSeparator);
                } else {
                    addWhiteNamespace(strbuffer, beforeDesSpan);
                    strbuffer.append(tmpStr.substring(i));
                    strbuffer.append(lineSeparator);
                }
            }
            strbuffer.append(lineSeparator);

        }

        return strbuffer.toString();
    }
    private int splitAndAppendText(StringBuffer buffer, String tmpStr, int idx, int lastIdx) {
        int origLast = lastIdx;
        while (lastIdx > idx && !Character.isWhitespace(tmpStr.charAt(lastIdx))) {
            --lastIdx;
        }
        if (lastIdx == idx) {
            lastIdx = origLast;
        }
        buffer.append(tmpStr.substring(idx, lastIdx));
        
        if (Character.isWhitespace(tmpStr.charAt(lastIdx))) {
            lastIdx++;
        }
        return lastIdx;
    }

    private void addWhiteNamespace(StringBuffer strbuffer, int count) {

        for (int i = 0; i < count; i++) {
            strbuffer.append(" ");
        }
    }

    public String getDetailedUsage(String id) {
        String result = null;
        Element element = toolspec.getElementById(id);
        
        List<Element> annotations = DOMUtils.findAllElementsByTagNameNS(element,
                                                                     Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                     "annotation");
        
        
        if ((annotations != null) && (annotations.size() > 0)) {
            result = annotations.get(0).getFirstChild().getNodeValue();
        }
        return result;
    }

    public String getToolUsage() {
        return toolspec.getAnnotation();
    }

}
