/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action.admin.cluster.snapshots.restore;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.client.Requests.restoreSnapshotRequest;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 * Restores a snapshot
 */
public class RestRestoreSnapshotAction extends BaseRestHandler {

    @Inject
    public RestRestoreSnapshotAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_snapshot/{repository}/{snapshot}/_restore", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        RestoreSnapshotRequest restoreSnapshotRequest = restoreSnapshotRequest(request.param("repository"), request.param("snapshot"));
        restoreSnapshotRequest.masterNodeTimeout(request.paramAsTime("master_timeout", restoreSnapshotRequest.masterNodeTimeout()));
        restoreSnapshotRequest.waitForCompletion(request.paramAsBoolean("wait_for_completion", false));
        restoreSnapshotRequest.source(request.content().toUtf8());
        client.admin().cluster().restoreSnapshot(restoreSnapshotRequest, new ActionListener<RestoreSnapshotResponse>() {
            @Override
            public void onResponse(RestoreSnapshotResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    response.toXContent(builder, request);
                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (IOException e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}
