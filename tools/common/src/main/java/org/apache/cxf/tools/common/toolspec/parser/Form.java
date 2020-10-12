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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.toolspec.Tool;


public class Form implements TokenConsumer {

    private static final Logger LOG = LogUtils.getL7dLogger(Form.class);
    private final Element element;

    private final List<Argument> arguments;
    private final List<OptionGroup> optionGroups;
    private final List<Option> options = new ArrayList<>();

    public Form(Element el) {
        this.element = el;

        optionGroups =
            DOMUtils.findAllElementsByTagNameNS(element,
                                                Tool.TOOL_SPEC_PUBLIC_ID,
                                                "optionGroup")
            .stream().map(elem -> new OptionGroup(elem)).collect(Collectors.toList());

        arguments =
            DOMUtils.findAllElementsByTagNameNS(element,
                                                Tool.TOOL_SPEC_PUBLIC_ID,
                                                "argument")
            .stream().map(elem -> new Argument(elem)).collect(Collectors.toList());

        Node node = el.getFirstChild();
        while (node != null) {
            if ("option".equals(node.getNodeName())) {
                options.add(new Option((Element)node));
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Attempt to consume all the args in the input stream by matching them to
     * options, optionGroups and argument specified in the usage definition.
     */
    public boolean accept(TokenInputStream args, Element result, ErrorVisitor errors) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Accepting token stream for form of usage: " + this + ", tokens are " + args);
        }
        int oldpos = args.getPosition();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Position is: " + oldpos);
        }
        boolean hasInfo = hasInfoOption(args);
        args.setPosition(oldpos);
        while (args.available() > 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Args is available");
            }
            boolean accepted = false;
            for (OptionGroup optionGroup : optionGroups) {
                if (optionGroup.accept(args, result, errors)) {
                    accepted = true;
                    break;
                }
            }

            if (!accepted) {
                for (Option option : options) {
                    if (option.accept(args, result, errors)) {
                        accepted = true;
                        break;
                    }
                }
            }

            if (!accepted) {
                break;
            }
        }

        for (OptionGroup optionGroup : optionGroups) {
            if (!optionGroup.isSatisfied(errors) && !hasInfo) {
                return false;
            }
        }

        for (Option option : options) {
            if (!option.isSatisfied(errors) && !hasInfo) {
                return false;
            }
        }

        for (Argument argument : arguments) {
            argument.accept(args, result, errors);
            if (!argument.isSatisfied(errors) && !hasInfo) {
                return false;
            }
        }

        if (args.available() > 0) {
            String next = args.peek();

            if (next.startsWith("-")) {
                errors.add(new ErrorVisitor.UnexpectedOption(next));
            } else {
                errors.add(new ErrorVisitor.UnexpectedArgument(next));
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(this + " form is returning false as there are more args available"
                         + " that haven't been consumed");
            }
            args.setPosition(oldpos);
            return false;
        }
        // If we have got here than we have fully consumed all the arguments.
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Form " + this + " is returning true");
        }
        return true;
    }

    public boolean hasInfoOption(TokenInputStream args) {
        int pos = args.getPosition();
        args.setPosition(0);
        String optionValue;
        while (args.available() > 0) {
            optionValue = args.read();
            if ("-?".equals(optionValue) || "-help".equals(optionValue) || "-h".equals(optionValue)
                || "-v".equals(optionValue)) {
                return true;
            }
        }
        args.setPosition(pos);
        return false;
    }

    public boolean isSatisfied(ErrorVisitor errors) {
        return true;
    }

    public String getName() {
        if (element.hasAttribute("value")) {
            return element.getAttribute("value");
        }
        return "default";

    }

    public String toString() {
        return getName();
    }

}
