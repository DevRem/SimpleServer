package ru.rem.server.session.client;

import java.nio.ByteBuffer;
import ru.rem.server.session.packets.AbstractPacket;

public interface ClientSession {
   
    public boolean isUpdateFileList();
   
    public void setUpdateFileList(boolean updateFileList);
    
    public String getAddress();

    public ByteBuffer getBuffer();

    public void setBufferSize(int size);

    public void setStatus(ClientStatus currentStatus);

    public ClientStatus getStatus();

    public void addQuery(AbstractPacket packetQuery);

    public AbstractPacket getLastQuery();

}
