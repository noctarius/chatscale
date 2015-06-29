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
package com.noctarius.chatscale.model;

import java.io.Serializable;

public class ChatMessage
        implements Serializable {

    private String origin;
    private String name;
    private String msg;
    private String msgId;

    public ChatMessage() {
    }

    public ChatMessage(String origin, String name, String msg, String msgId) {
        this.origin = origin;
        this.name = name;
        this.msg = msg;
        this.msgId = msgId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "name='" + name + '\'' +
                ", msg='" + msg + '\'' +
                ", msgId='" + msgId + '\'' +
                '}';
    }
}
