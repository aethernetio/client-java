package io.aether.cloud.client;

import io.aether.net.Command;

public class Temp {}
class Message{
    byte[] data;
    long time;
}
interface Server{
    @Command(1)
    Message[] getMessages();
}
interface Client{
    @Command(1)
    void pushMessages(Message[] messages);
}
