package aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AIOClientWriter implements CompletionHandler<Integer, AsynchronousSocketChannel> {
    private ByteBuffer byteBuffer;
    public AIOClientWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        System.out.println("A writer has been created!");
    }
    @Override
    public void completed(Integer result, AsynchronousSocketChannel socketChannel) {
        System.out.println("Message write successfully, size = " + result);
        System.out.println(String.format("Writer name : %s ", Thread.currentThread().getName()));
        byteBuffer.clear();
//        socketChannel.read(byteBuffer, socketChannel, new AIOClientReader(byteBuffer));
    }
    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
        System.out.println(exc);
        throw new RuntimeException(exc);
    }
}
