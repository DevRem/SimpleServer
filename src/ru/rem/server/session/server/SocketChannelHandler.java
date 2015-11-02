package ru.rem.server.session.server;

import java.io.File;
import java.io.FileFilter;
import ru.rem.server.session.client.ClientSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rem.server.session.actions.FileDownloader;
import ru.rem.server.session.actions.FileSender;
import ru.rem.server.session.actions.UpdatingFileList;
import ru.rem.server.session.assembly.PacketDataFileHandler;
import ru.rem.server.session.assembly.PacketDataHandler;
import ru.rem.server.session.assembly.PacketFileHandler;
import ru.rem.server.session.assembly.PacketHandler;
import ru.rem.server.session.assembly.ServerNavigation;
import ru.rem.server.session.client.ClientStatus;
import ru.rem.server.session.fileHandler.FileInfo;
import ru.rem.server.session.packets.AbstractPacket;
import ru.rem.server.session.packets.AbstractQuery;
import ru.rem.server.session.packets.AbstractResponse;
import ru.rem.server.session.packets.data.PacketAction;
import ru.rem.server.session.packets.data.PacketStatus;

public class SocketChannelHandler implements Runnable, ServerNavigation{
    
    private FileChannel channel;
    private SelectionKey key;
    private PacketHandler packetHandler;
    private SimpleServer mainServer;
    private ClientSession client;
    private BlockingQueue<FileInfo> fileListQueue;

    public SocketChannelHandler(ClientSession client, SimpleServer server) {
        this.client = client;
        this.mainServer = server;
        packetHandler = new PacketDataHandler(this);
        PacketHandler packetFileDataHandler = packetHandler.setNext(new PacketDataFileHandler(this));
        packetFileDataHandler.setNext(new PacketFileHandler(this));
    }
    
    @Override
    public void run() {
       if (key.isReadable()) {
           try {
               processRead();
           } catch (IOException ex) {
               Logger.getLogger(SocketChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
           }
       } else if (key.isWritable()) {
           try {
               processWrite();
           } catch (ClosedChannelException ex) {
               Logger.getLogger(SocketChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
           }
       } 
       getBuffer().clear();
       mainServer.selector.wakeup();
    }
    
    public ClientSession getClient(){
        return client;
    }
    
    public void processRead() throws IOException {

        SocketChannel socketChannel = getChannel();
        if (socketChannel.read(getBuffer()) <= 0) {
            updateClientStatus(ClientStatus.OFFLINE);
            return;
        }
        
        getBuffer().flip();
        
        AbstractPacket packet = getProcessingPacket(client.getLastQuery());
        int interestOps = 0;
        if (packet == null) {
            interestOps = recognizePacket();
        }else if(packet instanceof AbstractQuery){
            packetHandler.handleRead(packet, getBuffer());
            interestOps = packet.updateStatus(getChannel());
        }
        setOps(interestOps);
        
    }

    public void processWrite() throws ClosedChannelException{
        
        AbstractPacket packet = getProcessingPacket(client.getLastQuery());
        int interestOps = 0;
        if (packet == null) {
            interestOps = updateClientFileList();
        }else if(packet instanceof AbstractResponse){
            packetHandler.handleWrite(packet, getBuffer());
            interestOps = packet.updateStatus(getChannel());
        }
        setOps(interestOps);
        
    }
    
    @Override
    public SocketChannel getChannel(){
        return (SocketChannel) key.channel();
    }
    
    @Override
    public int send(ByteBuffer bb){
        try {
            bb.flip();
            return getChannel().write(bb);
        } catch (IOException ex) {
           // Проверить
        }
        return 0;
    }
    
    public void close(){
        try {
            if(channel != null && channel.isOpen()){
                channel.close();
                channel = null;
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int updateClientFileList(){
        if (client.isUpdateFileList() && !fileListQueue.isEmpty()) {
            try {
                FileInfo file = fileListQueue.take();
                return nextFile(file);
            } catch (InterruptedException ex) {
                Logger.getLogger(SocketChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return 0;
    }
    
    public int recognizePacket(){
        byte settings = getBuffer().get();
        AbstractPacket packet = determineFromAction(settings);
        int interestOps = 0;
        if(packet != null){
            addPacket(packet);
            getBuffer().rewind();
            packetHandler.handleRead(packet, getBuffer());
            interestOps = packet.updateStatus(getChannel());
        }
        return interestOps;
    }
    
    public AbstractPacket determineFromAction(byte settings){
        PacketAction action = PacketAction.getById(settings);
        PacketStatus status = PacketStatus.getById(settings);
        AbstractPacket packet = null;
        switch (action) {
            case UPLOAD:
                packet = new FileDownloader(status);
                break;
            case DOWNLOAD:
                packet = new FileSender(status);
                break;
        }
        return packet;
    }
    
    public int nextFile(FileInfo file){
        AbstractResponse packet = new UpdatingFileList(file);   
        addPacket(packet);
        packetHandler.handleWrite(packet, getBuffer());
        return packet.updateStatus(getChannel());
    }

    @Override
    public ByteBuffer getBuffer() {
        return client.getBuffer();
    }

    @Override
    public void addPacket(AbstractPacket packet) {
        client.addQuery(packet);
    }
    
    public final void fillQueueFileList() throws IOException {
        File path = new File(SimpleServer.PATH);
        if (!path.exists()) {
            throw new IOException("Cannot access " + SimpleServer.PATH + ". No such directory!");
        }
        File[] files = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        fileListQueue = new ArrayBlockingQueue<>(files.length);
        for(int index = files.length-1; index > -1; index--){
            fileListQueue.add(new FileInfo(files[index], index));
        }
    }
   
    public AbstractPacket getProcessingPacket(AbstractPacket packet){
        if (packet == null) {
            return null;
        }else if (PacketStatus.PROCESSED.equals(packet.getStatus())) {
            return packet;	
        } else {
            return getProcessingPacket(packet.getResponse());
        }
    }
    
    public void setOps(int interestOps){
        if (interestOps == 0) {
            interestOps = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        }
        key.interestOps(interestOps);
    }

    public void updateClientStatus(ClientStatus status) {
        
        switch (status) {
            case ONLINE:
                client.setUpdateFileList(true);
                try {
                    fillQueueFileList();
                } catch (IOException ex) {
                    Logger.getLogger(SocketChannelHandler.class.getName()).log(Level.SEVERE, "Не удалось получить список файлов сервера для клиента", ex);
                }
                break;
        }
        client.setStatus(status);
    }

    public void setSelectionkey(SelectionKey sk) {
        this.key = sk;
    }
    
}
