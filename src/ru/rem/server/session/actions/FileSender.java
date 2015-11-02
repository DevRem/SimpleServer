package ru.rem.server.session.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rem.server.session.fileHandler.PacketFile;
import ru.rem.server.session.packets.AbstractQuery;
import ru.rem.server.session.packets.AbstractResponse;
import ru.rem.server.session.packets.data.PacketAction;
import ru.rem.server.session.packets.data.PacketStatus;
import ru.rem.server.server.SimpleServer;

public class FileSender extends AbstractQuery {

    protected byte[] fileName;
    public int fileNameLength = 0;

    public FileSender(PacketStatus status) {
        this.status = status;
        action = PacketAction.DOWNLOAD;
        response = new ResponseDownload(action);
    }

    @Override
    public void prepareFileData(ByteBuffer bb) {
        try {
            int val = getAmountElements();
            if (val < fileNameLength) {
                for (int p = val; bb.remaining() > 0 && (fileNameLength - p) != 0; p++) {
                    fileName[p] = bb.get();
                }
            }
        } catch (RuntimeException ex) {
            Logger.getLogger(SimpleServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public int updateStatus(SocketChannel sc) {
        if (getDataProcessingResult() && getFileDataProcessingResult()) {
            status = PacketStatus.COMPLETE;
            return SelectionKey.OP_WRITE;
        } 
        
        if (!sc.isOpen() || !sc.isConnected()) {
            status = PacketStatus.INTERRUPTED;
        }
        return SelectionKey.OP_READ;
    }

    public int getAmountElements() {
        if (this.fileName == null) {
            return 0;
        }
        int count = 0;
        for (int b = 0; b < fileName.length; b++) {
            if (fileName[b] != 0) {
                count++;
            }
        }
        return count;
    }

    public int getRemainFileNameLength(int val) {
        int difference = fileNameLength - val;
        return difference;
    }

    @Override
    public boolean getFileDataProcessingResult() {
        return getAmountElements() == fileNameLength;
    }

    @Override
    public String toString(){
        return new String(fileName); 
    }

    @Override
    public void additionalData(ByteBuffer bb) {
        fileNameLength = bb.getInt();
        fileName = new byte[fileNameLength];
    }
    
    class ResponseDownload extends AbstractResponse implements PacketFile {

        protected FileChannel fileHandler;
        protected int fileNameCursor;
        protected long fileSize;
        protected long fileCursor;

        public ResponseDownload(PacketAction action) {
            this.action = action;
            this.status = PacketStatus.PROCESSED;
        }

        @Override
        public void prepareFileData(ByteBuffer bb) {
            try {
                for (; bb.hasRemaining() && fileNameCursor < fileName.length; fileNameCursor++) {
                    bb.put(fileName[fileNameCursor]);
                }
            } catch (RuntimeException ex) {
                Logger.getLogger(ResponseDownload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public int updateStatus(SocketChannel sc) {

            if (getDataProcessingResult() && getFileDataProcessingResult() && getFileProcessingResult()) {
                status = PacketStatus.COMPLETE;
                return SelectionKey.OP_READ;
            }
            if (!sc.isOpen() || !sc.isConnected()) {
                status = PacketStatus.INTERRUPTED;
                return SelectionKey.OP_READ;
            }
            return SelectionKey.OP_WRITE;
        }

        @Override
        public void incPosition(long val) {
            fileCursor += val;
        }

        @Override
        public long getPosition() {
            return fileCursor;
        }

        @Override
        public FileChannel getFileChannel() {
            return fileHandler;
        }

        @Override
        public boolean getFileProcessingResult() {
            return fileCursor == fileSize;
        }

        @Override
        public boolean getFileDataProcessingResult() {
            return fileNameCursor == fileName.length;
        }

        @Override
        public String toString() {
            return new String(fileName);
        }

        @Override
        public void additionalData(ByteBuffer bb) {
            try {
                this.fileHandler = new FileInputStream(new File(SimpleServer.PATH + (new String(fileName)))).getChannel();
                this.fileSize = fileHandler.size();
            } catch (IOException ex) {
                Logger.getLogger(ResponseDownload.class.getName()).log(Level.SEVERE, "Не удалось найти указанный файл", ex);
            }
            bb.putInt(fileName.length);
            bb.putLong(fileSize);
        }

    }

}
