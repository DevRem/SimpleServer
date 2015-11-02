package ru.rem.server.session.actions;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rem.server.session.fileHandler.FileInfo;
import ru.rem.server.session.packets.AbstractQuery;
import ru.rem.server.session.packets.AbstractResponse;
import ru.rem.server.session.packets.data.PacketAction;
import ru.rem.server.session.packets.data.PacketStatus;
import ru.rem.server.session.server.SimpleServer;


public class UpdatingFileList extends AbstractResponse {

    protected byte[] fileName;
    protected int fileNameCursor = 0;
    protected long fileLength;
    protected int index;
    
    public UpdatingFileList(FileInfo file) {
        this.status = PacketStatus.PROCESSED;
        this.action = PacketAction.UPDATE;
        this.fileName = file.getName().getBytes();
        this.fileLength = file.getSize();
        this.index = file.getIndex();
        response = new ResponseUpdateFileList();
    }

    @Override
    public void prepareFileData(ByteBuffer bb) {
        try {
            for (; bb.hasRemaining() && fileNameCursor < fileName.length; fileNameCursor++) {
                bb.put(fileName[fileNameCursor]);
            }
        } catch (RuntimeException ex) {
            Logger.getLogger(SimpleServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public int updateStatus(SocketChannel sc) {
        
        if(getDataProcessingResult() && getFileDataProcessingResult()){
            status = PacketStatus.COMPLETE;
            return SelectionKey.OP_READ;
        }
        
        if(!sc.isOpen() || !sc.isConnected()){
            status = PacketStatus.INTERRUPTED;
        }
        return SelectionKey.OP_WRITE;
    }

    @Override
    public boolean getFileDataProcessingResult() {
        return fileNameCursor == fileName.length;
    }

    @Override
    public void additionalData(ByteBuffer bb) {
        if (fileNameCursor == 0) {
            bb.putInt(fileName.length);
            bb.putLong(fileLength);
            bb.putInt(index);
        }
    }
    
    public class ResponseUpdateFileList extends AbstractQuery {

        public int index = -1;

        public ResponseUpdateFileList(){
            this.action = super.getAction();
        }
        
        @Override
        public PacketStatus getStatus() {
            return this.status;
        }

        @Override
        public PacketAction getAction() {
            return this.action;
        }


        @Override
        public void prepareFileData(ByteBuffer bb) {
            if(index == -1){
                index = bb.getInt();
            }
        }

        @Override
        public int updateStatus(SocketChannel sc) {
            if (this.getDataProcessingResult() && this.getFileDataProcessingResult()) {
                this.status = PacketStatus.COMPLETE;
                if(index == 0){
                    return SelectionKey.OP_READ;
                }
            } 
            
            if (!sc.isOpen() || !sc.isConnected()) {
                this.status = PacketStatus.INTERRUPTED;
                return SelectionKey.OP_READ;
            }
            return 0;
        }

        @Override
        public boolean getFileDataProcessingResult() {
            return index != -1;
        }

        @Override
        public void additionalData(ByteBuffer bb) {
            
        }

    }
    
}
