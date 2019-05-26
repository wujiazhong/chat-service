package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;

public class ChatClient {
    private Selector selector;
    private SocketChannel socketChannel;
    private SelectionKey sockKey;
    Scanner scanner = new Scanner(System.in);
    private Charset charset = Charset.forName("UTF-8");
    private static String EXIT = "!exit";

    public ChatClient(String serverIp, int port) {
        init(serverIp, port);
    }

    private void init(String serverIp, int port) {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(serverIp, port));
            selector = Selector.open();
            sockKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(new ClientThread()).start();
        ByteBuffer inputBuf = null;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.equals("")) {
                continue;
            }

            try {
                inputBuf = charset.encode(line);
                while (inputBuf.hasRemaining()) {
                    socketChannel.write(inputBuf);
                }
                inputBuf.clear();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (line.equals(EXIT)) {
                break;
            }
        }
        scanner.close();
    }

    private class ClientThread implements Runnable {
        public void run() {
            while (true) {
                try {
                    selector.select();
                    Iterator<SelectionKey> ite = selector.selectedKeys()
                            .iterator();
                    while (ite.hasNext()) {
                        SelectionKey key = ite.next();
                        ite.remove();
                        dealWithSelectionKey(key);
                    }
                }
                catch (IOException e) {
                    break;
                }
            }
        }

        private void dealWithSelectionKey(SelectionKey key) throws IOException {
            if (key.isValid()) {
                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    }
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);

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
                        if (content.equals(EXIT)) {
                            throw new IOException(EXIT);
                        }
                        System.out.println(content);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    catch (IOException e) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                        throw new IOException(e);
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        new ChatClient("localhost", 9999).start();
    }
}
