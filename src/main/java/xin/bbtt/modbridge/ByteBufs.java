package xin.bbtt.modbridge;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Minimal Minecraft-protocol primitive read/write helpers, so the FML handshake
 * logic does not depend on any particular mcprotocollib internal helper class.
 */
final class ByteBufs {

    private ByteBufs() {
    }

    static int readVarInt(ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = buf.readByte();
            value |= (current & 0x7F) << position;
            position += 7;
            if (position > 35) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((current & 0x80) != 0);
        return value;
    }

    static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    /** Reads a length-prefixed (VarInt) UTF-8 string. */
    static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Writes a length-prefixed (VarInt) UTF-8 string. */
    static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /** A ResourceLocation is wire-encoded exactly like a String ("namespace:path"). */
    static String readResourceLocation(ByteBuf buf) {
        return readString(buf);
    }

    static void writeResourceLocation(ByteBuf buf, String value) {
        writeString(buf, value);
    }

    static byte[] readByteArray(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    static byte[] toArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
}
