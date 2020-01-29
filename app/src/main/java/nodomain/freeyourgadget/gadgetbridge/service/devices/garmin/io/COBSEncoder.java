package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class COBSEncoder extends BufferedOutputStream {
	private static int MAX_SIZE = 0xFF;
	private int counter = 0;
	public COBSEncoder(OutputStream out) {
		super(out, MAX_SIZE + 1);
		try {
			out.write(0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public void write(int b) throws IOException {
		counter++;
		if (b == 0) {
			out.write(counter);
			flush();
			counter = 0;
			return;
		}
		super.write(b);
	}
	public void write(byte[] b, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			write(b[i]);
		}
	}
	public void close() throws IOException {
		out.write(0);
		super.close();
	}
	public static void main(String args[]) {
		try (OutputStream out = new COBSEncoder(System.out)) {
			int b;
			while ((b = System.in.read()) != -1) {
				out.write(b); // oh how I wish for .transferTo()
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

