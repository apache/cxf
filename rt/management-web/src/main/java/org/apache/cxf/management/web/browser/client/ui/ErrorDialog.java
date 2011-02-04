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

package org.apache.cxf.management.web.browser.client.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserResources;

/**
 * A dialog box showing an error message, when bad things happen.
 */
public class ErrorDialog extends PopupPanel {
    
    @Nonnull
    private final LogBrowserResources resources;

    @Nonnull
    private final LogBrowserConstans contants;

    @Nonnull
    private final Button closeButton;

    @Inject
    protected ErrorDialog(@Nonnull final LogBrowserResources resources,
                          @Nonnull final LogBrowserConstans contants) {
        super(/* auto hide */false, /* modal */true);

        this.resources = resources;
        this.contants = contants;
        this.closeButton = new Button();
    }

    /**
     * Create a dialog box to nicely format an exception.
     */
    public void setException(@Nonnull final Throwable exception) {
        String errorClassName = exception.getClass().getName();

        // Remove unwanted package (which begins on 'java.lang.' or 'org.apache.cxf.')
        if (errorClassName.startsWith("java.lang.")) {
            errorClassName = errorClassName.substring("java.lang.".length());
        } else if (errorClassName.startsWith("org.apache.cxf.")) {
            errorClassName = errorClassName.substring(errorClassName.lastIndexOf('.') + 1);
        }

        // Remove unwanted suffixes from class name (i.e. 'Exception', 'Error')
        if (errorClassName.endsWith("Exception")) {
            errorClassName = errorClassName.substring(0, errorClassName.length() - "Exception".length());
        } else if (errorClassName.endsWith("Error")) {
            errorClassName = errorClassName.substring(0, errorClassName.length() - "Error".length());
        }

        initializeLayout(errorClassName, exception.getMessage());
    }
    
    @Override
    public void center() {
        show();
        closeButton.setFocus(true);
    }

    private void initializeLayout(@Nonnull final String errorName, @Nullable final String errorMessage) {

        // Create error name and error message labels
        Label errorNameLabel = new Label(errorName);
        errorNameLabel.setStyleName(resources.css().errorDialogErrorType());

        Label errorMessageLabel = null;
        if (errorMessage != null && !"".equals(errorMessage)) {
            errorMessageLabel = new Label(errorMessage);
            DOM.setStyleAttribute(errorMessageLabel.getElement(), "whiteSpace", "pre");
        }

        // Initialize 'close' button
        closeButton.setText(contants.errorDialogContineButton());
        closeButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                hide();
            }
        });
        closeButton.addKeyPressHandler(new KeyPressHandler() {

            public void onKeyPress(KeyPressEvent event) {

                /*
                if the close button is triggered by a key we need to consume the key
                event, otherwise the key event would be propagated to the parent
                screen and eventually trigger some unwanted action there after the
                error dialog was closed
                 */
                event.stopPropagation();
            }
        });

        // Create 'title' label
        Label titleLabel = new Label();
        titleLabel.setStyleName(resources.css().errorDialogTitle());
        titleLabel.setText(contants.errorDialogTitle());

        // Create wrapper panel for 'error name' and 'error message' labels
        FlowPanel contentPanel = new FlowPanel();
        contentPanel.add(errorNameLabel);
        contentPanel.add(errorMessageLabel);

        // Create wrapper panel for 'close' button
        FlowPanel buttonsPanel = new FlowPanel();
        buttonsPanel.setStyleName(resources.css().errorDialogButtons());
        buttonsPanel.add(closeButton);

        // Create main panel and add all remain widgets
        FlowPanel centerPanel = new FlowPanel();
        centerPanel.add(titleLabel);
        centerPanel.add(contentPanel);
        centerPanel.add(buttonsPanel);

        // Add styles to window
        setGlassEnabled(true);
        getGlassElement().addClassName(resources.css().errorDialogGlass());
        addStyleName(resources.css().errorDialog());

        // Add main panel
        add(centerPanel);

        // Set window position
        int left = Window.getScrollLeft() + 20;
        int top = Window.getScrollTop() + 20;
        setPopupPosition(left, top);
    }
}
