package aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class ClientOnFuture {
    static final int DEFAULT_PORT = 7777;
    static final String IP = "127.0.0.1";
    static ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    public static void main(String[] args) {
        try (AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open()) {
            if (socketChannel.isOpen()) {
                //设置一些选项，非必选项，可使用默认设置
                socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                Void aVoid = socketChannel.connect(new InetSocketAddress(IP, DEFAULT_PORT)).get();
                //返回null表示连接成功
                if (aVoid == null) {
                    Integer messageLength = socketChannel.write(ByteBuffer.wrap("Hello Server！".getBytes())).get();
                    System.out.println(messageLength);
                    while (socketChannel.read(buffer).get() != -1) {
                        buffer.flip();//写入buffer之后，翻转，之后可以从buffer读取，或者将buffer内容写入通道
                        CharBuffer decode = Charset.defaultCharset().decode(buffer);
                        System.out.println(decode.toString());
                        if (buffer.hasRemaining()) {
                            buffer.compact();
                        } else {
                            buffer.clear();
                        }
                        int r = new Random().nextInt(1000);
                        if (r == 50) {
                            System.out.println("AIOClient closed!");
                            break;
                        } else {
                            socketChannel.write(ByteBuffer.wrap("Random number : ".concat(String.valueOf(r)).getBytes())).get();
                        }
                    }
                } else {
                    System.out.println("The connection cannot be established!");
                }
            } else {
                System.out.println("The asynchronous socket-channel cannot be opened!");
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println(e);
        }
    }
}
