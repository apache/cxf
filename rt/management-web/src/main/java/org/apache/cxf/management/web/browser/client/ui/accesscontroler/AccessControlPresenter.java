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

package org.apache.cxf.management.web.browser.client.ui.accesscontroler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.cxf.management.web.browser.client.EventBus;
import org.apache.cxf.management.web.browser.client.event.RemoteStorageAccessDeniedEvent;
import org.apache.cxf.management.web.browser.client.event.RemoteStorageAccessDeniedEventHandler;
import org.apache.cxf.management.web.browser.client.service.settings.Credentials;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade.StorageStrategy;
import org.apache.cxf.management.web.browser.client.ui.BasePresenter;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;

@Singleton
public class AccessControlPresenter extends BasePresenter implements AccessControlView.Presenter {

    @Nonnull
    private final AccessControlView view;

    @Nonnull
    private final SettingsFacade settingsFacade;

    @Inject
    public AccessControlPresenter(@Nonnull final EventBus eventBus,
            @Nonnull final AccessControlView view,
            @Nonnull @Named("BindStrategyForAccessControl") final BindStrategy bindStrategy,
            @Nonnull final SettingsFacade settingsFacade) {
        super(eventBus, view, bindStrategy);

        this.view = view;
        this.settingsFacade = settingsFacade;

        this.view.setPresenter(this);

        bind();
    }

    public void go(@Nonnull final HasWidgets container) {
        container.clear();
        container.add(view.asWidget());
    }

    public void onSignInButtonClicked() {
        String username = view.getUsername().getValue();
        String password = view.getPassword().getValue();

        if (isNotEmpty(username) && isNotEmpty(password)) {
            Boolean rememberMe = view.getRememberMe().getValue();
            Credentials credentials = new Credentials(username, password);
            if (rememberMe != null && rememberMe) {
                settingsFacade.initialize(StorageStrategy.LOCAL_AND_REMOTE, credentials);
            } else {
                settingsFacade.initialize(StorageStrategy.REMOTE, credentials);
            }
        }
    }

    private void bind() {
        registerHandler(eventBus.addHandler(RemoteStorageAccessDeniedEvent.TYPE,
            new RemoteStorageAccessDeniedEventHandler() {

                public void onRemoteStorageAccessDenied(RemoteStorageAccessDeniedEvent event) {
                    view.showAccessDeniedMessage();
                }
            }));
    }

    private boolean isNotEmpty(@Nullable final String str) {
        return str != null && !"".equals(str);
    }
}
