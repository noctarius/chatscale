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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.noctarius.chatscale.filter.CommandMessageFilter;
import com.noctarius.chatscale.model.ChatMessage;
import com.noctarius.chatscale.model.User;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerSession.RemoveListener;
import org.cometd.server.AbstractService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class ChannelHandler
        extends AbstractService {

    private final List<MessageFilter> messageFilters;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final ConcurrentMap<String, User> users;

    private final Cluster cluster;
    private final ITopic<ChatMessage> messageTopic;

    public ChannelHandler(BayeuxServer bayeux, HazelcastInstance hazelcastInstance) {
        super(bayeux, "ChannelHandler");

        List<MessageFilter> messageFilters = new ArrayList<>();
        messageFilters.add(new CommandMessageFilter(hazelcastInstance));
        this.messageFilters = Collections.unmodifiableList(messageFilters);

        cluster = hazelcastInstance.getCluster();
        users = hazelcastInstance.getMap("users");
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
        User user = users.get(session.getId());
        if (user == null) {
            return;
        }

        Map<String, Object> data = message.getDataAsMap();
        String msg = (String) data.get("msg");
        for (MessageFilter messageFilter : messageFilters) {
            String temp = messageFilter.filter(session, user, msg);
            if (temp == null) {
                return;
            }
            msg = temp;
        }
        if (msg == null) {
            return;
        }

        String msgId = UUID.randomUUID().toString();

        String name = user.getUsername();
        String color = user.getColor();

        publish(name, color, msg, msgId);
        broadcast(name, color, msg, msgId, session);
    }

    public void commandHandler(ServerSession session, ServerMessage message) {
        Map<String, Object> data = message.getDataAsMap();
        String command = (String) data.get("command");

        if ("login".equals(command)) {
            String username = (String) data.get("username");
            User user = new User(username, session.getId());
            User temp = users.putIfAbsent(session.getId(), user);
            if (temp == null) {
                session.addListener(new RemoveListener() {

                    @Override
                    public void removed(ServerSession session, boolean timeout) {
                        users.remove(session.getId());
                        session.removeListener(this);
                    }
                });
            } else {
                user = temp;
            }

            Map<String, Object> object = new HashMap<>();
            object.put("command", "login");
            object.put("user", gson.toJson(user));

            ServerMessage.Mutable response = getBayeux().newMessage();
            response.setChannel("/data/output");
            response.setData(object);

            session.deliver(session, response);
        }
    }

    private void broadcast(String name, String color, String msg, String msgId, ServerSession sender) {
        ServerMessage.Mutable message = getBayeux().newMessage();
        message.setChannel("/data/output");
        message.setData("<p><span style='color: " + color + "'>" + name + ":</span> " + msg + "</p>");
        message.setId(msgId);

        for (String sessionId : users.keySet()) {
            ServerSession serverSession = getBayeux().getSession(sessionId);
            if (serverSession != null) {
                serverSession.deliver(sender, message);
            }
        }

        System.out.println("broadcast {name: " + name + ", msg: " + msg + ", msgId: " + msgId + "}");
    }

    private void publish(String name, String color, String msg, String msgId) {
        ChatMessage chatMessage = new ChatMessage(cluster.getLocalMember().getUuid(), name, color, msg, msgId);
        messageTopic.publish(chatMessage);
        System.out.println("publish {name: " + name + ", msg: " + msg + ", msgId: " + msgId + "}");
    }

    private void messageTopicListener(Message<ChatMessage> message) {
        ChatMessage chatMessage = message.getMessageObject();
        String localId = cluster.getLocalMember().getUuid();
        if (!localId.equals(chatMessage.getOrigin())) {
            System.out.println("listener {message: " + chatMessage + "}");
            broadcast(chatMessage.getName(), chatMessage.getColor(), chatMessage.getMsg(), chatMessage.getMsgId(), null);
        }
    }

}
