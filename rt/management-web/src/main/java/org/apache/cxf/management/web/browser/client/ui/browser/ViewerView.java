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

import java.util.List;

import org.apache.cxf.management.web.browser.client.service.browser.Entry;
import org.apache.cxf.management.web.browser.client.service.browser.Links;
import org.apache.cxf.management.web.browser.client.ui.View;

public interface ViewerView extends View {

    public interface Presenter {
        void onEntryItemClicked(int row);

        void onOlderButtonClicked();

        void onNewerButtonClicked();

        void onLastButtonClicked();

        void onFirstButtonClicked();

        void onRefreshButtonClicked();                
    }

    void setMessageInsteadOfEntries(String message, String styleName);

    void setEntries(List<Entry> entries);

    void setEntryDetails(Entry entry);

    void setLinks(Links links);

    void setPresenter(Presenter presenter);
}
