package ru.rem.server.session.packets.data;

public enum PacketAction{
    
    DOWNLOAD(1), UPLOAD(2), UPDATE(3);

    public byte actionId;
    
    private PacketAction(int actionId) {
        this.actionId = (byte) (actionId << 4);
    }

    public byte getActionId() {
        return actionId;
    }
    
    public static PacketAction getById(byte id) {
        byte selectId = (byte)(id & 11110000);
        for(PacketAction val: values()){
            if(val.getActionId() == selectId){
                return val;
            }
        }
        return null;
    }
    
}
