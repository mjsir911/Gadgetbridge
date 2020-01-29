package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.io.DataInput;
import java.io.ObjectInput;
import java.io.FilterOutputStream;
import java.io.FilterInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.ByteOrder;
import java.io.IOException;

class ByteBufferObjectInputStream extends FilterInputStream implements ObjectInput {
	final public ByteBuffer buffer;
	ByteBufferObjectInputStream(InputStream in) {
		super(in);
		buffer = ByteBuffer.allocate(8); // 64 bits
	}

	private void readBytesToBuffer(int n) throws IOException {
		assert n <= 8;
		read(buffer.array(), 0, n);
	}

	public String readUTF() throws IOException {
		byte[] buf = new byte[readUnsignedByte()];
		read(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}
	public String readLine() throws IOException {
		throw new UnsupportedOperationException("readLine");
	}
	public double readDouble() throws IOException {
		readBytesToBuffer(Double.SIZE / 8);
		return buffer.getDouble(0);
	}
	public float readFloat() throws IOException {
		readBytesToBuffer(Float.SIZE / 8);
		return buffer.getFloat(0);
	}
	public long readLong() throws IOException {
		readBytesToBuffer(Long.SIZE / 8);
		return buffer.getLong(0);
	}
	public int readInt() throws IOException {
		readBytesToBuffer(Integer.SIZE / 8);
		return buffer.getInt(0);
	}
	public char readChar() throws IOException {
		readBytesToBuffer(Character.SIZE / 8);
		return buffer.getChar(0);
	}
	public short readShort() throws IOException {
		readBytesToBuffer(Short.SIZE / 8);
		return buffer.getShort(0);
	}
	public int readUnsignedShort() throws IOException {
		// return Short.toUnsignedInt(readShort());
		return readShort() & 0xFFFF;
	}
	public byte readByte() throws IOException {
		readBytesToBuffer(Byte.SIZE / 8);
		return buffer.get(0);
	}
	public int readUnsignedByte() throws IOException {
		// return Byte.toUnsignedInt(readByte());
		return readByte() & 0xFFFF;
	}
	public boolean readBoolean() throws IOException {
		throw new UnsupportedOperationException("readBoolean");
	}
	public int skipBytes(int n) throws IOException {
		return (int) skip(n);
	}
	public void readFully(byte[] b) throws IOException {
		if (read(b) != b.length) {
			throw new IOException("hi");
		}
	}
	public void readFully(byte[] b, int off, int len) throws IOException {
		if (read(b, off, len) != len) {
			throw new IOException("hello");
		}
	}
	public Object readObject() {
		throw new UnsupportedOperationException("readObject");
	}
}

