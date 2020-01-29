package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class CRCAdder extends CheckedOutputStream {
	public CRCAdder(OutputStream out, Checksum cksum) {
		super(out, cksum);
	}
	public void close() throws IOException {
		long crc = getChecksum().getValue();
		out.write((int) crc >> 4);
		out.write((byte) crc & 0xFF);
		super.close();
	}
	public static void main(String args[]) {
		try (OutputStream out = new CRCAdder(System.out, new CRC32())) {
			int b;
			while ((b = System.in.read()) != -1) {
				out.write(b); // oh how I wish for .transferTo()
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

