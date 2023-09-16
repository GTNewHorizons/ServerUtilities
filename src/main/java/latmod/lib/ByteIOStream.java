package latmod.lib;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.UUID;

/**
 * Made by LatvianModder
 */
public final class ByteIOStream implements DataInput, DataOutput {

    protected byte[] bytes;
    protected int pos;

    private static void throwUTFException(String s) {
        try {
            throw new UTFDataFormatException(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ByteIOStream(int size) {
        bytes = new byte[size];
    }

    public ByteIOStream() {
        this(0);
    }

    public int getDataPos() {
        return pos;
    }

    public int available() {
        return bytes.length - pos;
    }

    public byte[] toByteArray() {
        if (pos == bytes.length) return bytes;
        byte[] b = new byte[pos];
        System.arraycopy(bytes, 0, b, 0, pos);
        return b;
    }

    public byte[] toCompressedByteArray() {
        try {
            return ByteCompressor.compress(bytes, 0, pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void expand(int c) {
        if (pos + c >= bytes.length) {
            byte[] b = new byte[bytes.length + Math.max(c, 16)];
            System.arraycopy(bytes, 0, b, 0, pos);
            bytes = b;
        }
    }

    public void flip() {
        pos = 0;
    }

    public void setData(byte[] b) {
        bytes = b;
        flip();
    }

    public void setCompressedData(byte[] b) {
        try {
            setData(ByteCompressor.decompress(b, 0, b.length));
        } catch (Exception e) {
            e.printStackTrace();
            setData(null);
        }
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean compressed) {
        byte[] b = compressed ? toCompressedByteArray() : toByteArray();
        return "[ (" + b.length + ") " + LMStringUtils.stripB(b) + " ]";
    }

    public OutputStream createOutputStream() {
        return new OutputStream() {

            public void write(int b) throws IOException {
                ByteIOStream.this.write(b);
            }

            public void write(byte b[], int off, int len) throws IOException {
                ByteIOStream.this.write(b, off, len);
            }
        };
    }

    public InputStream createInputStream() {
        return new InputStream() {

            public int read() throws IOException {
                return (available() <= 0) ? -1 : ByteIOStream.this.readUnsignedByte();
            }

            public int read(byte b[], int off, int len) throws IOException {
                ByteIOStream.this.readFully(b, off, len);
                return len;
            }

            public int available() {
                return ByteIOStream.this.available();
            }
        };
    }

    // Read functions //

    public byte readByte() {
        byte b = bytes[pos];
        pos++;
        return b;
    }

    public void readFully(byte[] b, int off, int len) {
        if (b == null || len == 0) return;
        System.arraycopy(bytes, pos, b, off, len);
        pos += len;
    }

    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public void readFully(byte[] b) {
        readFully(b, 0, b.length);
    }

    public byte[] readByteArray(ByteCount c) {
        int s = c.read(this);
        if (s == -1) return null;
        byte[] b = new byte[s];
        readFully(b);
        return b;
    }

    public boolean readBoolean() {
        return readUnsignedByte() == 1;
    }

    public char readChar() {
        return (char) readUnsignedShort();
    }

    public String readUTF() {
        int l = readUnsignedShort();
        if (l == 65535) return null;
        else if (l == 0) return "";

        char[] utf_chars = new char[l];

        int c, c2, c3, c1 = 0, cac = 0, pos0 = pos;

        pos += l;

        while (c1 < l) {
            c = (int) bytes[c1 + pos0] & 0xFF;
            if (c > 127) break;
            c1++;
            utf_chars[cac++] = (char) c;
        }

        while (c1 < l) {
            c = (int) bytes[c1 + pos0] & 0xFF;

            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    c1++;
                    utf_chars[cac++] = (char) c;
                    break;
                case 12:
                case 13:
                    c1 += 2;
                    if (c1 > l) throwUTFException("malformed input: partial character at end");
                    c2 = (int) bytes[c1 + pos0 - 1];
                    if ((c2 & 0xC0) != 0x80) throwUTFException("malformed input around byte " + c1);
                    utf_chars[cac++] = (char) (((c & 0x1F) << 6) | (c2 & 0x3F));
                    break;
                case 14:
                    c1 += 3;
                    if (c1 > l) throwUTFException("malformed input: partial character at end");
                    c2 = (int) bytes[c1 + pos0 - 2];
                    c3 = (int) bytes[c1 + pos0 - 1];
                    if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80))
                        throwUTFException("malformed input around byte " + (c1 - 1));
                    utf_chars[cac++] = (char) (((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | ((c3 & 0x3F)));
                    break;
                default:
                    throwUTFException("malformed input around byte " + c1);
            }
        }

        String s = new String(utf_chars, 0, cac);
        utf_chars = null;
        return s;
    }

    @Deprecated
    public String readLine() {
        return null;
    }

    public int readUnsignedShort() {
        int v = Bits.toUShort(bytes, pos);
        pos += 2;
        return v;
    }

    public short readShort() {
        return (short) readUnsignedShort();
    }

    public int readInt() {
        int v = Bits.toInt(bytes, pos);
        pos += 4;
        return v;
    }

    public long readLong() {
        long v = Bits.toLong(bytes, pos);
        pos += 8;
        return v;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public UUID readUUID() {
        UUID v = Bits.toUUID(bytes, pos);
        pos += 16;
        return v;
    }

    public int[] readIntArray(ByteCount c) {
        int len = c.read(this);
        if (len == -1) return null;
        int[] ai = new int[len];
        for (int i = 0; i < len; i++) ai[i] = readInt();
        return ai;
    }

    // Write functions //

    public void writeByte(int i) {
        expand(1);
        bytes[pos] = (byte) i;
        pos++;
    }

    public void write(int b) {
        writeByte(b);
    }

    public void write(byte[] b, int off, int len) {
        if (b == null || len == 0) return;
        expand(len);
        System.arraycopy(b, off, bytes, pos, len);
        pos += len;
    }

    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public void writeByteArray(byte[] b, ByteCount c) {
        if (b == null) {
            c.write(this, -1);
            return;
        }
        c.write(this, b.length);
        write(b);
    }

    public void writeBoolean(boolean b) {
        writeByte(b ? 1 : 0);
    }

    public void writeChar(int c) {
        writeShort(c);
    }

    public void writeUTF(String s) {
        if (s == null) {
            writeShort(-1);
            return;
        }
        int sl = s.length();
        if (sl == 0) {
            writeShort(0);
            return;
        }
        int l = 0;
        int c;

        for (int i = 0; i < sl; i++) {
            c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) l++;
            else if (c > 0x07FF) l += 3;
            else l += 2;
        }

        if (l >= 65535) throwUTFException("encoded string too long: " + l + " bytes");

        writeShort(l);
        expand(l);

        int i = 0;
        for (i = 0; i < sl; i++) {
            c = s.charAt(i);
            if (!(c >= 0x0001 && c <= 0x007F)) break;
            writeByte(c);
        }

        for (; i < sl; i++) {
            c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) writeByte(c);
            else if (c > 0x07FF) {
                writeByte(0xE0 | ((c >> 12) & 0x0F));
                writeByte(0x80 | ((c >> 6) & 0x3F));
                writeByte(0x80 | ((c) & 0x3F));
            } else {
                writeByte(0xC0 | ((c >> 6) & 0x1F));
                writeByte(0x80 | ((c) & 0x3F));
            }
        }
    }

    public void writeBytes(String s) {
        if (s == null || s.isEmpty()) return;
        for (int i = 0; i < s.length(); i++) writeByte((byte) s.charAt(i));
    }

    public void writeChars(String s) {
        if (s == null || s.isEmpty()) return;
        for (int i = 0; i < s.length(); i++) writeChar(s.charAt(i));
    }

    public void writeShort(int s) {
        expand(2);
        Bits.fromUShort(bytes, pos, s);
        pos += 2;
    }

    public void writeInt(int i) {
        expand(4);
        Bits.fromInt(bytes, pos, i);
        pos += 4;
    }

    public void writeLong(long l) {
        expand(8);
        Bits.fromLong(bytes, pos, l);
        pos += 8;
    }

    public void writeFloat(float f) {
        writeInt(Float.floatToIntBits(f));
    }

    public void writeDouble(double d) {
        writeLong(Double.doubleToLongBits(d));
    }

    public void writeUUID(UUID uuid) {
        expand(16);
        Bits.fromUUID(bytes, pos, uuid);
        pos += 16;
    }

    public void writeIntArray(int[] ai, ByteCount c) {
        int asize = (ai == null) ? -1 : ai.length;
        c.write(this, asize);
        for (int i = 0; i < asize; i++) writeInt(ai[i]);
    }

    public int skipBytes(int n) {
        return 0;
    }

    public static int getUTFLength(String data) {
        if (data == null) return -1;
        else if (data.isEmpty()) return 0;
        else {
            int len = 0;
            char c;

            for (int i = 0; i < data.length(); i++) {
                c = data.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) len++;
                else if (c > 0x07FF) len += 3;
                else len += 2;
            }

            return len;
        }
    }
}
