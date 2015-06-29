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

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.noctarius.chatscale.model.ChatMessage;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerSession.RemoveListener;
import org.cometd.server.AbstractService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelHandler
        extends AbstractService {

    private final List<String> users = new CopyOnWriteArrayList<>();

    private final Cluster cluster;
    private final ITopic<ChatMessage> messageTopic;

    public ChannelHandler(BayeuxServer bayeux, HazelcastInstance hazelcastInstance) {
        super(bayeux, "ChannelHandler");

        cluster = hazelcastInstance.getCluster();
        messageTopic = hazelcastInstance.getTopic("messages");
        messageTopic.addMessageListener(this::messageTopicListener);

        // Register as channel handler
        addService("/data/input", "messageHandler");
        addService("/data/command", "commandHandler");

        // Make it a persistent channel
        bayeux.getChannel("/data/input").setPersistent(true);
        bayeux.getChannel("/data/command").setPersistent(true);
    }

    public void messageHandler(ServerSession session, ServerMessage message) {
        Map<String, Object> data = message.getDataAsMap();
        String name = (String) data.get("name");
        String msg = (String) data.get("msg");

        String msgId = UUID.randomUUID().toString();

        publish(name, msg, msgId);
        broadcast(name, msg, msgId, session);
    }

    public void commandHandler(ServerSession session, ServerMessage message) {
        Map<String, Object> data = message.getDataAsMap();
        String command = (String) data.get("command");

        if ("login".equals(command)) {
            users.add(session.getId());
            session.addListener(new RemoveListener() {

                @Override
                public void removed(ServerSession session, boolean timeout) {
                    users.remove(session.getId());
                }
            });
        }
    }

    private void broadcast(String name, String msg, String msgId, ServerSession sender) {
        ServerMessage.Mutable message = getBayeux().newMessage();
        message.setChannel("/data/output");
        message.setData("<p><span color='red'>" + name + ":</span> " + msg + "</p>");
        message.setId(msgId);

        for (String sessionId : users) {
            ServerSession serverSession = getBayeux().getSession(sessionId);
            serverSession.deliver(sender, message);
        }

        System.out.println("broadcast {name: " + name + ", msg: " + msg + ", msgId: " + msgId + "}");
    }

    private void publish(String name, String msg, String msgId) {
        ChatMessage chatMessage = new ChatMessage(cluster.getLocalMember().getUuid(), name, msg, msgId);
        messageTopic.publish(chatMessage);
        System.out.println("publish {name: " + name + ", msg: " + msg + ", msgId: " + msgId + "}");
    }

    private void messageTopicListener(Message<ChatMessage> message) {
        ChatMessage chatMessage = message.getMessageObject();
        String localId = cluster.getLocalMember().getUuid();
        if (!localId.equals(chatMessage.getOrigin())) {
            System.out.println("listener {message: " + chatMessage + "}");
            broadcast(chatMessage.getName(), chatMessage.getMsg(), chatMessage.getMsgId(), null);
        }
    }

}
