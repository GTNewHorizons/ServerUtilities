package latmod.lib;

public enum ByteCount {

    BYTE(1),
    SHORT(2),
    INT(4);

    public final int bytes;

    ByteCount(int i) {
        bytes = i;
    }

    public void write(ByteIOStream io, int num) {
        if (this == BYTE) io.writeByte(num);
        else if (this == SHORT) io.writeShort(num);
        else io.writeInt(num);
    }

    public int read(ByteIOStream io) {
        if (this == BYTE) {
            byte b = io.readByte();
            if (b == -1) return -1;
            return b & 0xFF;
        } else if (this == SHORT) {
            short s = io.readShort();
            if (s == -1) return -1;
            return s & 0xFFFF;
        }
        return io.readInt();
    }
}
