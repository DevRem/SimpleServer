package ru.rem.server.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rem.server.session.client.Client;
import ru.rem.server.session.client.ClientStatus;

public class SimpleServer implements Runnable{

    public final int PORT = 8888;
    public final int THREADS = 3;
    public final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    public ServerFrame serverFrame;
    public ServerSocketChannel ssc;
    public Selector selector;
    public static String PATH = "";
    
    public SimpleServer(ServerFrame serverFrame){
        this.serverFrame = serverFrame;
    }
    
    @Override
    public void run() {
        try {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(PORT));
            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                if (selector.isOpen()) {
                    selector.select();
                    processSelectedKeys(selector.selectedKeys());
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(SimpleServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void processSelectedKeys(Set<SelectionKey> keys) throws IOException{
        Iterator<SelectionKey> itr = keys.iterator();
        while (itr.hasNext()) {
            SelectionKey key = itr.next();
            itr.remove();
            if (key.isAcceptable()) {
                processAccept(key);
            }else{
                processReadAndWrite(key);
            }
        }
    }
    
    private void processAccept(SelectionKey key) throws IOException{
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        SocketChannel sc = server.accept();
        sc.configureBlocking(false);
        SelectionKey skClient = sc.register(selector, SelectionKey.OP_WRITE);
        SocketAddress adress = sc.getLocalAddress(); 
        SocketChannelHandler fileHandler =  serverFrame.getUserList().getSession(adress);
        if(fileHandler == null){
            fileHandler = new SocketChannelHandler(new Client(adress), this);
            serverFrame.addSession(adress, fileHandler);
        }
        fileHandler.updateClientStatus(ClientStatus.ONLINE);
        fileHandler.setSelectionkey(skClient);
        skClient.attach(fileHandler);
    }
    
    private void processReadAndWrite(SelectionKey key){
        key.interestOps(0);
        SocketChannelHandler fileHandler = (SocketChannelHandler) key.attachment();
        executor.submit(fileHandler);
    }
    
    public synchronized void closeChannel(SelectionKey sk) throws IOException {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        if (socketChannel.isConnected()) {
            socketChannel.close();
        }
        sk.cancel();
    }

}
