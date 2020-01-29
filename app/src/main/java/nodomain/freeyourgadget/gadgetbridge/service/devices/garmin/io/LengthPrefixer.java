package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;

public class LengthPrefixer extends BufferedOutputStream {
	OutputStream out;
	int length = 2; // magic
	public LengthPrefixer(OutputStream out) {
		super(out, 0xFFFF); // magic
		this.out = out;
	}
	public void write(int b) throws IOException {
		super.write(b);
		length++;
	}
	public void write(byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
		length += len;
	}
	public void flush() throws IOException {
		// go around the buffer to write at the beginning
		out.write(length & 0xFF);
		out.write(length >> 8);
		super.flush();
	}

	public static void main(String args[]) {
		try (OutputStream out = new LengthPrefixer(System.out)) {
			int b;
			while ((b = System.in.read()) != -1) {
				out.write(b); // oh how I wish for .transferTo()
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

