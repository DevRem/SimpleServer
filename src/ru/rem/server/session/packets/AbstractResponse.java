package ru.rem.server.session.packets;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rem.server.server.SimpleServer;

public abstract class AbstractResponse extends AbstractPacket{
    
    protected boolean dataSent;

    @Override
    public void prepareData(ByteBuffer bb) {
	try {
            bb.put((byte) (status.getStatusId() | action.getActionId()));
            additionalData(bb);
            dataSent = true;
        } catch (RuntimeException ex) {
            Logger.getLogger(SimpleServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    @Override
    public boolean getDataProcessingResult() {
        return dataSent;
    }
    
    public abstract void additionalData(ByteBuffer bb);
    
}
