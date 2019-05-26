package aio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

public class AIOClientReader implements CompletionHandler<Integer, ByteBuffer> {
    AsynchronousSocketChannel aSyncChannel;
    Charset charset = Charset.forName("utf-8");


    public AIOClientReader(AsynchronousSocketChannel channel) {
        this.aSyncChannel = channel;
    }
    @Override
    public void completed(Integer result, ByteBuffer byteBuffer) {
        try {
            SocketAddress localAddress = aSyncChannel.getLocalAddress();
            SocketAddress remoteAddress = aSyncChannel.getRemoteAddress();
            System.out.println("localAddress : " + localAddress.toString());
            System.out.println("remoteAddress : " + remoteAddress.toString());
        } catch (IOException e) {
            System.out.println(e);
        }
        byteBuffer.flip();
        StringBuilder sb = new StringBuilder();
        while (byteBuffer.hasRemaining()) {
            sb.append(charset.decode(byteBuffer));
        }

        System.out.println("Receive message from client : " + sb.toString());
        byteBuffer.clear();
        aSyncChannel.read(byteBuffer, byteBuffer, this);
    }
    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        System.out.println(exc);
        throw new RuntimeException(exc);
    }
}
