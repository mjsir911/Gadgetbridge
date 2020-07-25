package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class CRCAdder extends CheckedOutputStream {
	protected DataOutput out;

	public <T extends OutputStream & DataOutput> CRCAdder(T out, Checksum cksum) {
		super(out, cksum);
		this.out = out;
	}

	// public CRCAdder(OutputStream out, Checksum cksum) {
	// 	this(new DataOutputStream(out), cksum);
	// }

	public void close() throws IOException {
		long crc = getChecksum().getValue();
		// the crc size here can't be easily abstracted out
		out.writeShort((short) crc);
		super.close();
	}

	public static void main(String args[]) {
		try (OutputStream out = new CRCAdder(new DataOutputStream(System.out), new CRC32())) {
			int b;
			while ((b = System.in.read()) != -1) {
				out.write(b); // oh how I wish for .transferTo()
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

