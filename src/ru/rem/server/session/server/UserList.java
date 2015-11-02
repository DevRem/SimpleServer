package ru.rem.server.session.server;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import ru.rem.server.session.client.ClientSession;

public class UserList extends AbstractTableModel{
    
    public Map<SocketAddress, SocketChannelHandler> clientSessions = new HashMap<>();
    
    public UserList() {
        super();
    }
    
    public void addSession(SocketAddress clientAdress, SocketChannelHandler sc){
        clientSessions.put(clientAdress, sc);
    }
    
    public SocketChannelHandler getSession(SocketAddress clientAdress){
        return clientSessions.get(clientAdress);
    }
    
    @Override
    public int getRowCount() {
        return clientSessions.size();
    }
    @Override
    public int getColumnCount() {
        return 2;
    }
    @Override
    public Object getValueAt(int row, int col) {
        Object[] entries = clientSessions.entrySet().toArray();
        Map.Entry entry = (Map.Entry) entries[row];
        ClientSession client = ((SocketChannelHandler) entry.getValue()).getClient();
        if (col == 0) {
            String ad = client.getAddress();
            return ad;
        } else if (col == 1 ) {
            return client.getStatus();
        } else {
            throw new IndexOutOfBoundsException("Количество колонок превышает допустимое: 2 < " + col);
        }   
    }
    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "IP";
            case 1:
                return "Статус";
            default:
                return "Неопределено";
        }
    }

}