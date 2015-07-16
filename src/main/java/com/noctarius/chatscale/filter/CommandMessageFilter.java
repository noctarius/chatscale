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
package com.noctarius.chatscale.filter;

import com.hazelcast.core.HazelcastInstance;
import com.noctarius.chatscale.Command;
import com.noctarius.chatscale.MessageFilter;
import com.noctarius.chatscale.command.ColorCommand;
import com.noctarius.chatscale.model.User;
import org.cometd.bayeux.server.ServerSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandMessageFilter
        implements MessageFilter {

    private final Map<String, Command> commands;

    public CommandMessageFilter(HazelcastInstance hazelcastInstance) {
        Map<String, Command> commands = new HashMap<>();
        commands.put("color", new ColorCommand(hazelcastInstance));
        this.commands = Collections.unmodifiableMap(commands);
    }

    @Override
    public String filter(ServerSession session, User user, String message) {
        char first = message.charAt(0);
        if ('/' == first) {
            String[] token = message.substring(1).split(" ");

            Command command = commands.get(token[0]);
            if (command == null) {
                return "<p>Command not found</p>";
            }

            String[] args = new String[token.length - 1];
            System.arraycopy(token, 1, args, 0, token.length - 1);
            return command.execute(session, user, args);
        }
        return message;
    }
}
