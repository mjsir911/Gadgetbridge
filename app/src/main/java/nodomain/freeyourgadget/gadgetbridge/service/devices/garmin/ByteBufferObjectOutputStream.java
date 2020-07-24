package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.io.ObjectOutput;
import java.io.FilterOutputStream;
import java.io.Externalizable;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ByteBufferObjectOutputStream extends FilterOutputStream implements ObjectOutput {  
	final public ByteBuffer buffer;
	public ByteBufferObjectOutputStream(OutputStream out) {
		super(out);
		buffer = ByteBuffer.allocate(8);
	}

	private void writeBytesFromBuffer(int n) throws IOException {
		assert n <= 8;
		write(buffer.array(), 0, n);
		buffer.clear();
	}

	public void writeUTF(String s) throws IOException {
		writeByte(s.length());
		write(s.getBytes(StandardCharsets.UTF_8));
	}
	public void writeChars(String s) throws IOException {
		throw new UnsupportedOperationException("writeChars");
	}
	public void writeBytes(String s) throws IOException {
		throw new UnsupportedOperationException("writeBytes");
	}
	public void writeDouble(double v) throws IOException {
		buffer.putDouble(v);
		writeBytesFromBuffer(Double.SIZE / 8);
	}
	public void writeFloat(float v) throws IOException {
		buffer.putFloat(v);
		writeBytesFromBuffer(Float.SIZE / 8);
	}
	public void writeLong(long v) throws IOException {
		buffer.putLong(v);
		writeBytesFromBuffer(Long.SIZE / 8);
	}
	public void writeInt(int v) throws IOException {
		buffer.putInt(v);
		writeBytesFromBuffer(Integer.SIZE / 8);
	}
	public void writeShort(int v) throws IOException {
		buffer.putShort((short) v);
		writeBytesFromBuffer(Short.SIZE / 8);
	}
	public void writeChar(int v) throws IOException {
		buffer.putChar((char) v);
		writeBytesFromBuffer(Character.SIZE / 8);
	}
	public void writeByte(int v) throws IOException {
		buffer.put((byte) v);
		writeBytesFromBuffer(Byte.SIZE / 8);
	}
	public void writeBoolean(boolean v) throws IOException {
		throw new UnsupportedOperationException("writeBoolean");
	}
	public void writeObject(Object v) throws IOException {
		((Externalizable) v).writeExternal(this);
	}
}
