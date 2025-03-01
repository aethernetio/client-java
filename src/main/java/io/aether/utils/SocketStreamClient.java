package io.aether.utils;

import io.aether.common.NetworkConfigurator;
import io.aether.utils.streams.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SocketStreamClient implements NodeDown<byte[], byte[]> {
    private final Node<byte[], byte[], byte[], byte[]> subProtocol;
    private final Thread thread;
    private final Deque<Value<byte[]>> buffer = new ConcurrentLinkedDeque<>();
    private volatile Socket socket;
    private volatile OutputStream socketOutput;
    private volatile InputStream socketInput;
    private volatile State state = State.CONNECT;

    public SocketStreamClient(URI uri, NetworkConfigurator configurator) {
        subProtocol = configurator.initConnectionClient(uri);
        subProtocol.down().link(socketGate.outSide());
        thread = new Thread(new Runnable() {
            byte[] buffer2 = new byte[1000];

            private void work() {
                Thread t = Thread.currentThread();
                try {
                    if (t.isInterrupted() || state == State.STOP) {
                        state = State.STOP;
                        try {
                            socket.close();
                        } catch (Exception ignored) {
                        }
                        return;
                    }
                    if (socketInput.available() <= 0) return;
                    var len = socketInput.read(buffer2);
                    if (len <= 0) {
                        return;
                    } else if (len == buffer2.length) {
//                        System.out.println("socket read: " + Arrays.toString(buffer2));
                        socketGate.inSide.send(Value.ofForce(buffer2));
                        buffer2 = new byte[buffer2.length];
                    } else {
                        var b = Arrays.copyOf(buffer2, len);
//                        System.out.println("socket read: " + Arrays.toString(b));
                        socketGate.inSide.send(Value.ofForce(b));
                    }
                } catch (IOException e) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                    socket = null;
                    socketOutput = null;
                    socketInput = null;
                    if (state != State.STOP) state = State.CONNECT;
                }
            }

            private void connect() {
                try {
                    socket = new Socket(uri.getHost(), uri.getPort());
                    socketOutput = socket.getOutputStream();
                    socketInput = socket.getInputStream();
                    state = State.WORK;
                } catch (Exception e) {
                    state = State.CONNECT;
                    RU.sleep(100);
                    return;
                }
                flushBuffer();
            }

            private void stop() {
                state = State.STOP;
                var s = socket;
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        RU.error(e);
                    }
                }
            }

            @Override
            public void run() {
                var t = Thread.currentThread();
                while (!t.isInterrupted() && state != State.STOP) {
                    switch (state) {
                        case CONNECT:
                            connect();
                            break;
                        case WORK:
                            work();
                            break;
                        case STOP:
                            stop();
                            return;
                    }
                    flushBuffer();
                    Thread.yield();
                }

            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void flushBuffer() {
        socketGate.inSide.requestData();
        if (state != State.WORK) return;
        while (true) {
            Value<byte[]> value = buffer.poll();
            if (value == null) return;
            try {
                boolean force = value.force();
                if (value.data() != null) {
                    socketOutput.write(value.data());
                    value = null;
                }
                if (force) {
                    socketOutput.flush();
                }
                socketGate.inSide.requestData();
            } catch (IOException e) {
                if (value != null) {
                    buffer.addFirst(value);
                }
                state = State.CONNECT;
                break;
            }
        }
    }    private final FGate<byte[], byte[], Acceptor<byte[]>> socketGate = FGate.of(new Acceptor<>(this) {

        @Override
        public boolean isSoftWritable() {
            return state == State.WORK && socket.isConnected();
        }

        @Override
        public String toString() {
            return socket.toString();
        }

        @Override
        public boolean isWritable() {
            return true;
        }


        @Override
        public void send(Value<byte[]> value) {
            buffer.add(value);
            flushBuffer();
        }

        @Override
        public void close() {
            state = State.STOP;
            thread.interrupt();
        }

        @Override
        public void requestData() {
        }

    });

    @Override
    public String toString() {
        return socket.toString();
    }

    @Override
    public FGate<byte[], byte[], ?> gUp() {
        return subProtocol.gUp();
    }

    enum State {
        CONNECT, WORK, STOP
    }




}
