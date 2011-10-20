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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.cxf.management.web.browser.client.event.SelectedSubscriptionEvent;
import org.apache.cxf.management.web.browser.client.event.SelectedSubscriptionEventHandler;
import org.apache.cxf.management.web.browser.client.service.browser.Feed;
import org.apache.cxf.management.web.browser.client.service.browser.FeedProxyImpl;
import org.apache.cxf.management.web.browser.client.ui.BasePresenter;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserResources;

@Singleton
public class ViewerPresenter extends BasePresenter implements ViewerView.Presenter {

    @Nonnull
    private final FeedProxyImpl proxy;

    @Nonnull
    private Feed feed;

    @Nonnull
    private final ViewerView view;

    @Nonnull
    private final LogBrowserConstans constans;

    @Nonnull
    private final LogBrowserResources resources;

    @Inject
    public ViewerPresenter(@Nonnull final EventBus eventBus,
            @Nonnull final ViewerView view,
            @Nonnull @Named("BindStrategyForViewer") final BindStrategy bindStrategy,
            @Nonnull final FeedProxyImpl proxy,
            @Nonnull final LogBrowserConstans constans,
            @Nonnull final LogBrowserResources resources) {
        super(eventBus, view, bindStrategy);

        this.view = view;
        this.view.setPresenter(this);

        this.proxy = proxy;
        this.constans = constans;
        this.resources = resources;        

        setFeed(Feed.EMPTY);

        bind();
    }

    private void bind() {
        eventBus.addHandler(SelectedSubscriptionEvent.TYPE, new SelectedSubscriptionEventHandler() {

            public void onSelectedSubscription(SelectedSubscriptionEvent event) {
                getFeed(event.getUrl());
            }
        });
        
    }

    public void onEntryItemClicked(final int row) {
        assert row >= 0 && row < feed.getEntries().size();
        view.setEntryDetails(feed.getEntries().get(row));
    }

    public void onNewerButtonClicked() {
        getFeed(feed.getLinks().getNext());
    }

    public void onLastButtonClicked() {
        getFeed(feed.getLinks().getLast());
    }

    public void onFirstButtonClicked() {
        getFeed(feed.getLinks().getFirst());
    }

    public void onRefreshButtonClicked() {
        getFeed(feed.getLinks().getSelf());
    }

    public void onOlderButtonClicked() {
        getFeed(feed.getLinks().getPrevious());
    }

    private void setFeed(@Nonnull final Feed newFeed) {
        feed = newFeed;

        view.setEntryDetails(null);
        view.setLinks(feed.getLinks());

        if (feed.getEntries().isEmpty()) {
            setNoEntriesMessage();
        } else {
            view.setEntries(feed.getEntries());
        }
    }

    private void setNoEntriesMessage() {
        view.setMessageInsteadOfEntries(constans.browserTabNoEntries(),
            resources.css().browserTabNoEntriesMessage());
    }

    private void setLoadingMessage() {
        view.setMessageInsteadOfEntries(constans.browserTabLoading(),
            resources.css().browserTabLoadingMessage());
    }

    //TODO Rename this method. Name should emphasize that it gets and sets feed
    private void getFeed(@Nonnull final String url) {
        setLoadingMessage();
        proxy.getFeed(url, new FeedProxyImpl.Callback() {

            @Override
            public void onSuccess(@Nonnull final Feed newFeed) {
                setFeed(newFeed);
            }

            @Override
            public void onError(@Nonnull final Request request, @Nonnull final Throwable ex) {
                setFeed(Feed.EMPTY);
                super.onError(request, ex);
            }

        });
    }
}
