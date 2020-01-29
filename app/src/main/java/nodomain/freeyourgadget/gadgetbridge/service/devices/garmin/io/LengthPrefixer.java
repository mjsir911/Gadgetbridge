package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.OutputStream;
import java.io.BufferedOutputStream;

public class LengthPrefixer extends BufferedOutputStream {
	public LengthPrefixer(OutputStream out) {
		super(out, 1024);
	}
	public static void main(String args[]) {
		
	}
}

