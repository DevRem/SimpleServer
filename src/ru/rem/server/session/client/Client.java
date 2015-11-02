package ru.rem.server.session.client;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import ru.rem.server.session.packets.AbstractPacket;

public class Client implements ClientSession{

    private ByteBuffer buf = ByteBuffer.allocate(1024*2);
    public ClientStatus currentStatus = ClientStatus.OFFLINE;
    public final SocketAddress ipAdress;
    public List<AbstractPacket> packets = new ArrayList<>();
    public boolean updateFileList = true;

    @Override
    public boolean isUpdateFileList() {
        return updateFileList;
    }

    @Override
    public void setUpdateFileList(boolean updateFileList) {
        this.updateFileList = updateFileList;
    }
    
    public Client(SocketAddress ipAdress){
        this.ipAdress = ipAdress;
    }
    
    @Override
    public String getAddress(){
        return ipAdress.toString();
    }
    
    @Override
    public void addQuery(AbstractPacket packetQuery){
        packets.add(packetQuery);
    }
    
    @Override
    public AbstractPacket getLastQuery(){
        if(packets.isEmpty()){
            return null;
        }
        return packets.get(packets.size()-1);
    }
    
    /*@Override
    public int hashCode() {
        return ipAdress.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Client) {
            return ipAdress.equals(((Client) o).ipAdress);
        }
        return false;
    }*/
    
    @Override
    public final void setBufferSize(int size) {
       buf = ByteBuffer.allocate((size < 24 ? 24 : size));   
    }
    
    @Override
    public void setStatus(ClientStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    @Override
    public ClientStatus getStatus(){
        return currentStatus;
    }
    
    @Override
    public ByteBuffer getBuffer() {
       return buf;
    }

}

