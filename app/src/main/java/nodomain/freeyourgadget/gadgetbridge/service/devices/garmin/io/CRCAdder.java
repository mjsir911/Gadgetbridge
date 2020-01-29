package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

public class CRCAdder extends CheckedOutputStream {
	public CRCAdder(OutputStream out, Checksum cksum) {
		super(out, cksum);
	}
	public static void main(String args[]) {
		
	}
}

