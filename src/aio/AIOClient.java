package aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIOClient {
    private static String DEFAULT_HOST = "127.0.0.1";
    private static int DEFAULT_PORT = 7777;
    ExecutorService executor = Executors.newFixedThreadPool(1);
    AsynchronousSocketChannel aSyncChannel;

    public AIOClient() {
        // 以指定线程池来创建一个AsynchronousChannelGroup
        AsynchronousChannelGroup channelGroup =
                null;
        try {
            channelGroup = AsynchronousChannelGroup.withThreadPool(executor);
            aSyncChannel = AsynchronousSocketChannel.open(channelGroup);
            aSyncChannel.connect(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT)).get();
            System.out.println("--- 与服务器连接成功 ---");
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }

    public void start(){
        final ByteBuffer buff = ByteBuffer.allocate(1024);
        buff.clear();
        aSyncChannel.read(buff, buff, new AIOClientReader(aSyncChannel));
    }

    //向服务器发送消息
    public void sendMsg(String msg) {
        ByteBuffer writeBuf = ByteBuffer.wrap(msg.getBytes());
        aSyncChannel.write(writeBuf, null, new AIOClientWriter(writeBuf));
    }

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception{
        AIOClient client = new AIOClient();
        client.start();
        System.out.println("请输入请求消息：");
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNext()) {
            String nextLine = scanner.nextLine();
            if (nextLine.equals("")) {
                continue;
            }
            client.sendMsg(nextLine);
        }
        scanner.close();
    }
}
