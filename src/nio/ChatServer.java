package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ChatServer {
    private Selector selector;

    private Charset charset = Charset.forName("UTF-8");

    private static int TIMEOUT = 1024 * 1000;

    private static String SPLIT_CHAR = ":";

    private static String AT_CHAR = "@";

    private static String USER_EXIST = "Username existed！";

    private static String EXIT = "!exit";

    private Map<SocketChannel, String> allChannel = new HashMap<>();

    public ChatServer(int port) {
        init(port);
    }

    private void init(int port) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel
                    .open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                if (selector.select(TIMEOUT) == 0){
                    continue;
                }
                Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
                while (ite.hasNext()) {
                    SelectionKey key = ite.next();
                    ite.remove();
                    dealWithSelectionKey(key);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dealWithSelectionKey(SelectionKey key) {
        if (key.isValid()) {
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                try {
                    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                    key.interestOps(SelectionKey.OP_ACCEPT);
                    channel.write(charset.encode("Input username："));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (key.isReadable()) {
                SocketChannel channel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
                StringBuilder sb = new StringBuilder();
                try {
                    while (channel.read(buffer) > 0) {
                        buffer.flip();
                        sb.append(charset.decode(buffer));
                        buffer.clear();
                    }
                    String content = sb.toString();
                    System.out.println(channel.getRemoteAddress() + "\t" + content);
                    if (content.length() > 0) {
                        String name = allChannel.get(channel);
                        if (name == null) {
                            if (allChannel.containsValue(content)) {
                                channel.write(charset.encode(USER_EXIST));
                            } else {
                                allChannel.put(channel, content);
                                channel.write(charset
                                        .encode("昵称设置完毕！现在您可以聊天了。群聊直接输入要说的内容即可，私聊的话，先输入"
                                                + AT_CHAR + "，然后输入对方的昵称，再输入"
                                                + SPLIT_CHAR + "，最后输入所要说的内容即可。如："
                                                + AT_CHAR + "robin" + SPLIT_CHAR
                                                + "Hi robin,I'm James."));
                                channel.write(charset
                                        .encode("现在共有在线人数为：" + allChannel.size()));
                                sendMessageToAll(content, content + "上线了，现在共有在线人数为："
                                        + allChannel.size());
                            }
                        } else {
                            if (content.equals(EXIT)) {
                                throw new IOException(EXIT);
                            }
                            boolean isAt = content.contains(AT_CHAR);
                            if (isAt) {
                                String header = content.split(SPLIT_CHAR)[0];
                                String to = header
                                        .substring(header.indexOf(AT_CHAR) + 1);
                                String msg = content
                                        .substring(content.indexOf(SPLIT_CHAR) + 1);
                                msg = name + "对你说：" + msg;
                                sendMessageToPrivate(to, msg);
                            } else {
                                String msg = name + "说：" + content;
                                sendMessageToAll(name, msg);
                            }
                        }
                    }
                    key.interestOps(SelectionKey.OP_READ);
                } catch (IOException e) {
                    String name = allChannel.get(channel);
                    try {
                        if (name == null) {
                            System.out.println(
                                    channel.getRemoteAddress() + "\t还没输入昵称就强行关闭了。");
                        } else {
                            ByteBuffer msgBuf = charset.encode(EXIT);
                            while (msgBuf.hasRemaining() && channel.isConnected()) {
                                channel.write(msgBuf);
                            }
                            msgBuf.clear();
                            allChannel.remove(channel);
                            sendMessageToAll(name,
                                    name + "下线了，现在共有在线人数为：" + allChannel.size());
                        }
                        channel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendMessageToAll(String from, String msg) {
        for (Map.Entry<SocketChannel, String> entry : allChannel.entrySet()) {
            SocketChannel channel = entry.getKey();
            String name = entry.getValue();
            if (!name.equals(from)) {
                try {
                    channel.write(charset.encode(msg));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessageToPrivate(String to, String msg) {
        for (Map.Entry<SocketChannel, String> entry : allChannel.entrySet()) {
            SocketChannel channel = entry.getKey();
            String name = entry.getValue();
            if (name.equals(to)) {
                try {
                    channel.write(charset.encode(msg));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChatServer(9999).start();
    }
}
