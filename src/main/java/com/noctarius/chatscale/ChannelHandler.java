/*
 * Copyright (c) 2015, Christoph Engelbert (aka noctarius) and
 * contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.noctarius.chatscale;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;

import java.util.Map;
import java.util.UUID;

public class ChannelHandler
        extends AbstractService {

    public ChannelHandler(BayeuxServer bayeux) {
        super(bayeux, "ChannelHandler");

        // Register as channel handler
        addService("/data/input", "messageHandler");

        // Make it a persistent channel
        bayeux.getChannel("/data/input").setPersistent(true);
    }

    public void messageHandler(ServerSession session, ServerMessage message) {
        Map<String, Object> data = message.getDataAsMap();
        String name = (String) data.get("name");
        String msg = (String) data.get("msg");

        ServerMessage.Mutable response = getBayeux().newMessage();
        response.setChannel("/data/output");
        response.setData("<p><span color='red'>" + name + ":</span> " + msg + "</p>");
        response.setId(UUID.randomUUID().toString());

        session.deliver(session, response);
    }

}
