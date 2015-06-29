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

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.CometDServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
            throws Exception {

        LOGGER.info("Starting ChatScale...");

        int port = Integer.getInteger("chatscale.port");
        Server server = new Server(port);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        ServletContextHandler context = new ServletContextHandler(
                contexts, "/", ServletContextHandler.SESSIONS);

        BayeuxServerImpl bayeuxServer = new BayeuxServerImpl();
        context.setAttribute(BayeuxServer.ATTRIBUTE, bayeuxServer);

        ServletHolder channel = context.addServlet(CometDServlet.class, "/channel/*");
        channel.setInitParameter("timeout", "20000");
        channel.setInitParameter("interval", "100");
        channel.setInitParameter("maxInterval", "10000");
        channel.setInitParameter("multiFrameInterval", "5000");
        channel.setInitParameter("logLevel", "3");
        channel.setInitOrder(1);

        ServletHolder resource = context.addServlet(DefaultServlet.class, "/static/*");
        resource.setInitParameter("resourceBase", findStaticContentBase());
        channel.setInitOrder(2);

        server.setHandler(contexts);
        server.setStopAtShutdown(true);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        server.start();

        new ChannelHandler(bayeuxServer, hazelcastInstance);

        server.join();
    }

    private static String findStaticContentBase() {
        String path = System.getProperty("user.dir");
        path = path.replace("\\", "/");
        return path.endsWith("/") ? path : path + "/";
    }

}
