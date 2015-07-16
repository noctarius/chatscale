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
package com.noctarius.chatscale.command;

import com.hazelcast.core.HazelcastInstance;
import com.noctarius.chatscale.Command;
import com.noctarius.chatscale.model.User;
import org.cometd.bayeux.server.ServerSession;

import java.util.Map;

public class ColorCommand
        implements Command {

    private final Map<String, User> users;

    public ColorCommand(HazelcastInstance hazelcastInstance) {
        users = hazelcastInstance.getMap("users");
    }

    @Override
    public String execute(ServerSession session, User user, String... args) {
        if (args.length != 1) {
            return "<p>Invalid number of arguments</p>";
        }

        String color = args[0];
        user.setColor(color);
        users.put(session.getId(), user);

        return null;
    }
}
