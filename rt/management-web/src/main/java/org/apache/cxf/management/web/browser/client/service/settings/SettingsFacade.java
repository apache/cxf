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

package org.apache.cxf.management.web.browser.client.service.settings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.cxf.management.web.browser.client.EventBus;
import org.apache.cxf.management.web.browser.client.event.GoToBrowserEvent;
import org.apache.cxf.management.web.browser.client.event.RemoteStorageAccessDeniedEvent;

@Singleton
public class SettingsFacade {

    @Nonnull
    private final RemoteStorageProxy remoteStorage;

    @Nonnull
    private final LocalStorage localStorage;

    @Nonnull
    private final EventBus eventBus;

    @Nullable
    private StorageLayer storageLayer;

    @Nonnull
    private final IdentifierGenerator identifierGenerator;

    private boolean initialized;

    public enum StorageStrategy {
        LOCAL_AND_REMOTE,
        REMOTE
    }    

    @Inject
    public SettingsFacade(@Nonnull final RemoteStorageProxy remoteStorage,
                          @Nonnull final LocalStorage localStorage,
                          @Nonnull final EventBus eventBus,
                          @Nonnull final IdentifierGenerator identifierGenerator) {
        this.remoteStorage = remoteStorage;
        this.localStorage = localStorage;
        this.eventBus = eventBus;
        this.identifierGenerator = identifierGenerator;
    }

    public boolean isSettingsAlreadyInLocalStorage() {
        return localStorage.isAvailable() && localStorage.retrieveSettings() != null;
    }    

    public void initialize(@Nonnull final StorageStrategy strategy, @Nonnull final Credentials credentials) {
        storageLayer = createStorageLayers(strategy);
        storageLayer.initialize(credentials);
        initialized = true;
    }

    public void clearMemoryAndLocalStorage() {
        assert storageLayer != null;
        storageLayer.clear();
        initialized = false;
    }

    public void addSubscription(@Nonnull final String name, @Nonnull final String url) {
        assert !"".equals(name);
        assert !"".equals(url);
        assert storageLayer != null;
        isValid();

        String id = identifierGenerator.generateUUID();
        assert id != null && !"".equals(id);

        storageLayer.getSettings().getSubscriptions().add(new Subscription(id, name, url));
        storageLayer.update();
    }

    public void updateSubscription(@Nonnull final Subscription subscription) {
        assert subscription.getUrl() != null && !"".equals(subscription.getUrl());
        assert storageLayer != null;
        isValid();

        storageLayer.getSettings().getSubscriptions().remove(subscription);
        storageLayer.getSettings().getSubscriptions().add(subscription);
        storageLayer.update();
    }

    public void removeSubscription(@Nonnull final Subscription subscription) {
        assert subscription.getUrl() != null && !"".equals(subscription.getUrl());
        assert storageLayer != null;
        isValid();

        storageLayer.getSettings().getSubscriptions().remove(subscription);
        storageLayer.update();
    }

    @Nonnull
    public List<Subscription> getSubscriptions() {
        assert storageLayer != null;
        isValid();

        return new ArrayList<Subscription>(storageLayer.getSettings().getSubscriptions());
    }

    private void isValid() {
        if (!initialized) {
            throw new IllegalStateException("Storage layers not initialized");
        }
    }

    @Nonnull
    private StorageLayer createStorageLayers(@Nonnull final StorageStrategy storageStrategy) {
        switch(storageStrategy) {
        case LOCAL_AND_REMOTE:
            return new RemoteStorageLayer(remoteStorage,
                new LocalStorageLayer(localStorage, new MemoryStorageLayer()));
        case REMOTE:
            return new RemoteStorageLayer(remoteStorage, new MemoryStorageLayer());
        default:
            throw new IllegalArgumentException("Unknown storage strategy type");
        }
    }

    private interface StorageLayer {

        boolean initialize(Credentials credentials);

        Settings getSettings();

        void update(Settings settings);

        void update();

        void clear();
    }

    private static class MemoryStorageLayer implements StorageLayer {

        @Nonnull
        private Settings settings;

        public boolean initialize(@Nonnull final Credentials credentials) {
            this.settings = new Settings();
            this.settings.setCredentials(credentials);
            return false;
        }

        @Nonnull
        public Settings getSettings() {
            return this.settings;
        }

        public void update(@Nonnull final Settings newSettings) {
            this.settings = newSettings;
        }

        public void update() {
        }

        public void clear() {
            this.settings = new Settings();
        }
    }

    private static class LocalStorageLayer implements StorageLayer {

        @Nonnull
        private final MemoryStorageLayer parent; // TODO change to StorageLayer interface

        @Nonnull
        private final LocalStorage localStorage;

        public LocalStorageLayer(@Nonnull final LocalStorage localStorage,
                                 @Nonnull final MemoryStorageLayer parent) {
            this.parent = parent;
            this.localStorage = localStorage;
        }

        public boolean initialize(@Nonnull final Credentials credentials) {
            boolean isSuccess = parent.initialize(credentials);
            
            assert !isSuccess;

            Settings settings = localStorage.retrieveSettings();
            if (settings != null) {
                parent.update(settings);
                return true;
            } else {
                return false;
            }
        }

        @Nonnull
        public Settings getSettings() {
            return parent.getSettings();
        }

        public void update(@Nonnull final Settings settings) {
            parent.update(settings);
            localStorage.saveSettings(settings);
        }

        public void update() {
            update(parent.getSettings());
        }

        public void clear() {
            parent.clear();
            localStorage.clear();
        }
    }

    private class RemoteStorageLayer implements StorageLayer {

        @Nonnull
        private final StorageLayer parent;

        @Nonnull
        private final RemoteStorageProxy remoteStorage;

        public RemoteStorageLayer(@Nonnull final RemoteStorageProxy remoteStorage,
                                  @Nonnull final StorageLayer parent) {
            this.parent = parent;
            this.remoteStorage = remoteStorage;
        }

        public boolean initialize(@Nonnull final Credentials credentials) {
            assert credentials != null;

            boolean isSuccess = parent.initialize(credentials);
            
            if (!isSuccess) {
                remoteStorage.saveSettings(credentials, new RemoteStorageProxyImpl.Callback() {

                    @Override
                    public void onAccessDenied() {
                        eventBus.fireEvent(new RemoteStorageAccessDeniedEvent());
                    }

                    @Override
                    public void onSuccess(@Nullable final Settings retrievedSettings) {
                        Settings settings = retrievedSettings != null ? retrievedSettings : new Settings();
                        settings.setCredentials(credentials);

                        parent.update(settings);

                        eventBus.fireEvent(new GoToBrowserEvent());
                    }
                });
            }

            return true;
        }

        @Nonnull
        public Settings getSettings() {
            return parent.getSettings();
        }

        public void update(@Nonnull final Settings settings) {
            assert settings != null;

            parent.update(settings);

            remoteStorage.retrieveSettings(settings.getCredentials(), settings,
                new RemoteStorageProxyImpl.NoActionCallback());
        }
        
        public void update() {
            update(parent.getSettings());
        }

        public void clear() {
            parent.clear();
        }
    }
}

