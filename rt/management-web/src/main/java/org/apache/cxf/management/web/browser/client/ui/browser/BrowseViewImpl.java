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

package org.apache.cxf.management.web.browser.client.ui.browser;

import javax.annotation.Nonnull;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BrowseViewImpl extends Composite implements BrowseView {

    @UiTemplate("BrowseView.ui.xml")
    interface BrowseViewUiBinder extends UiBinder<Widget, BrowseViewImpl> { }

    private static final BrowseViewUiBinder UI_BINDER = GWT.create(BrowseViewUiBinder.class);

    @UiField @Nonnull
    FlowPanel navigationHeaderSlot;

    @UiField @Nonnull
    FlowPanel navigationSidebarSlot;    

    @UiField @Nonnull
    FlowPanel viewerSlot;

    @Inject
    public BrowseViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));

    }

    public HasWidgets getNaviagationHeaderSlot() {
        return navigationHeaderSlot;
    }

    public HasWidgets getNaviagationSidebarSlot() {
        return navigationSidebarSlot;
    }

    public HasWidgets getViewerSlot() {
        return viewerSlot;
    }

    @Nonnull
    public Widget asWidget() {
        return this;
    }
}
