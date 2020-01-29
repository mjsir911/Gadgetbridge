package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class CRCChecker extends CheckedInputStream {
	public CRCChecker(InputStream in, Checksum cksum) {
		// super(new BufferedInputStream(in, 2), cksum);
		super(in, cksum);
	}
	private int getThirdNext() throws IOException {
		in.mark(3);
		in.read();
		in.read();
		int ret = in.read();
		in.reset();
		return ret;
	}
	public int read() throws IOException {
		// if (getThirdNext() == -1) {
		// 	int crc = in.read() | (in.read() << 8);
		// 	if (crc != getChecksum().getValue()) {
		// 		// throw new IOException("Invalid CRC Detected");
		// 	}
		// 	System.err.println("0x" + Integer.toHexString(crc));
		// 	return -1;
		// }
		return super.read();
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
		try (InputStream in = new CRCChecker(System.in, new CRC32())) {
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

