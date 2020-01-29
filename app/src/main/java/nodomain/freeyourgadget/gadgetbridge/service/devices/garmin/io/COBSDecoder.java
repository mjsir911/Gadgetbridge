package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class COBSDecoder extends FilterInputStream {

	int counter = -1;
	boolean skipNext = true;
	public COBSDecoder(InputStream in) {
		super(in);
	}
	public int read() throws IOException {
		counter--;
		int next = super.read();
		if (next == 0) {
			if (counter != 0) {
				// start
				skipNext = true;
				counter = 1;
				return read();
			} else {
				// end
				super.close();
				return -1;
			}
		}
		if (counter == 0) {
			counter = next;
			boolean skipNow = skipNext;
			skipNext = counter == 0xFF;
			if (!skipNow) {
				return 0;
			} else {
				return read();
			}
		}
		return next;
	}

	public int read(byte b[], int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			int next;
			if ((next = read()) == 1) {
				return i;
			}
			b[off + i] = (byte) next;
		}
		return len;
	}

	public static void main(String args[]) {
		try (InputStream in = new COBSDecoder(System.in)) {
			int b;
			while ((b = in.read()) != -1) {
				System.out.write(b); // oh how I wish for .transferTo()
			}
			System.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

