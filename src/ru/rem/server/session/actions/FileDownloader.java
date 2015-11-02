package ru.rem.server.session.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import ru.rem.server.session.server.SimpleServer;

public class FileDownloader extends AbstractQuery implements PacketFile {

    protected FileChannel fileHandler;
    protected byte[] fileName = null;
    protected int fileNameLength = 0;
    protected int fileNameCursor = 0;
    protected long fileLength = 0;
    protected long position = 0;

    public FileDownloader(PacketStatus status) {
        this.status = status;
        this.action = PacketAction.UPLOAD;
        response = new ResponseDownload(action);
    }

    @Override
    public boolean getFileDataProcessingResult() {
        return getAmountElements() == fileNameLength;
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

    @Override
    public int updateStatus(SocketChannel sc) {
        if (getDataProcessingResult() && getFileDataProcessingResult() && getFileProcessingResult()) {
            status = PacketStatus.COMPLETE;
            return SelectionKey.OP_WRITE;
        } 
        if (!sc.isOpen() || !sc.isConnected()) {
            status = PacketStatus.INTERRUPTED;
        }
        return SelectionKey.OP_READ;
    }

    @Override
    public void incPosition(long val) {
        position += val;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public FileChannel getFileChannel() {
        if (fileHandler == null) {
            File file = new File(SimpleServer.PATH + new String(fileName));
            try {
                this.fileHandler = new FileOutputStream(file).getChannel();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fileHandler;
    }

    @Override
    public boolean getFileProcessingResult() {
        return getPosition() == fileLength;
    }

    @Override
    public void additionalData(ByteBuffer bb) {
        fileNameLength = bb.getInt();
        fileLength = bb.getLong();
        fileName = new byte[fileNameLength];
    }

    public class ResponseDownload extends AbstractResponse {

        public ResponseDownload(PacketAction action) {
            this.action = action;
            fileNameCursor = 0;
        }
        
        @Override
        public void prepareFileData(ByteBuffer bb) {
            try {
                for (; bb.hasRemaining() && fileNameCursor < fileName.length; fileNameCursor++) {
                    bb.put(fileName[fileNameCursor]);
                }
            } catch (RuntimeException ex) {
                Logger.getLogger(FileDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public int updateStatus(SocketChannel sc) {

            if (this.getDataProcessingResult() && this.getFileDataProcessingResult()) {
                this.status = PacketStatus.COMPLETE;
                return SelectionKey.OP_READ;
            }
            if (!sc.isOpen() || !sc.isConnected()) {
                this.status = PacketStatus.INTERRUPTED;
                return SelectionKey.OP_READ;
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
            }
        }
        
    }

}
