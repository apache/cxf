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

package org.apache.cxf.management.web.browser.client;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.name.Names;

import org.apache.cxf.management.web.browser.client.service.settings.IdentifierGenerator;
import org.apache.cxf.management.web.browser.client.service.settings.IdentifierGeneratorImpl;
import org.apache.cxf.management.web.browser.client.service.settings.LocalStorage;
import org.apache.cxf.management.web.browser.client.service.settings.LocalStorageImpl;
import org.apache.cxf.management.web.browser.client.service.settings.RemoteStorageProxy;
import org.apache.cxf.management.web.browser.client.service.settings.RemoteStorageProxyImpl;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;
import org.apache.cxf.management.web.browser.client.ui.DialogBindStrategyImpl;
import org.apache.cxf.management.web.browser.client.ui.WidgetBindStrategyImpl;
import org.apache.cxf.management.web.browser.client.ui.accesscontroler.AccessControlView;
import org.apache.cxf.management.web.browser.client.ui.accesscontroler.AccessControlViewImpl;
import org.apache.cxf.management.web.browser.client.ui.browser.BrowseView;
import org.apache.cxf.management.web.browser.client.ui.browser.BrowseViewImpl;
import org.apache.cxf.management.web.browser.client.ui.browser.EditCriteriaView;
import org.apache.cxf.management.web.browser.client.ui.browser.EditCriteriaViewImpl;
import org.apache.cxf.management.web.browser.client.ui.browser.NavigationSidebarView;
import org.apache.cxf.management.web.browser.client.ui.browser.NavigationSidebarViewImpl;
import org.apache.cxf.management.web.browser.client.ui.browser.ViewerView;
import org.apache.cxf.management.web.browser.client.ui.browser.ViewerViewImpl;
import org.apache.cxf.management.web.browser.client.ui.common.NavigationHeaderView;
import org.apache.cxf.management.web.browser.client.ui.common.NavigationHeaderViewImpl;
import org.apache.cxf.management.web.browser.client.ui.settings.SettingsView;
import org.apache.cxf.management.web.browser.client.ui.settings.SettingsViewImpl;
import org.apache.cxf.management.web.browser.client.ui.settings.SubscriptionDialog;
import org.apache.cxf.management.web.browser.client.ui.settings.SubscriptionDialogImpl;

public class Module extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(EventBus.class).to(DefaultEventBus.class);
        bind(IdentifierGenerator.class).to(IdentifierGeneratorImpl.class);
        bind(AccessControlView.class).to(AccessControlViewImpl.class);
        bind(SettingsView.class).to(SettingsViewImpl.class);
        bind(SubscriptionDialog.class).to(SubscriptionDialogImpl.class);
        bind(BrowseView.class).to(BrowseViewImpl.class);
        bind(NavigationHeaderView.class).to(NavigationHeaderViewImpl.class);
        bind(NavigationSidebarView.class).to(NavigationSidebarViewImpl.class);
        bind(EditCriteriaView.class).to(EditCriteriaViewImpl.class);
        bind(ViewerView.class).to(ViewerViewImpl.class);
        bind(RemoteStorageProxy.class).to(RemoteStorageProxyImpl.class);
        bind(LocalStorage.class).to(LocalStorageImpl.class);

        //TODO move it to view class:

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForAccessControl"))
                .to(WidgetBindStrategyImpl.class);

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForBrowser"))
                .to(WidgetBindStrategyImpl.class);

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForNavigationHeader"))
                .to(WidgetBindStrategyImpl.class);

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForEditCriteria"))
                .to(DialogBindStrategyImpl.class);        

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForNavigationSidebar"))
                .to(WidgetBindStrategyImpl.class);

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForSettings"))
                .to(WidgetBindStrategyImpl.class);

        bind(BindStrategy.class)
                .annotatedWith(Names.named("BindStrategyForViewer"))
                .to(WidgetBindStrategyImpl.class);
        
    }

}
