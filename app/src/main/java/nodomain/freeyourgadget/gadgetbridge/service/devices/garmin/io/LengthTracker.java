package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.DataInput;
import java.io.IOException;

public class LengthTracker extends FilterInputStream {
	private int counter;
	private boolean start = true;
	private DataInput in;

	public <T extends InputStream & DataInput> LengthTracker(T in) {
		super(in);
		this.in = in;
	}

	private void startCounter() throws IOException {
		// no way to abstract out length size, it's a short
		counter = in.readShort();
	}

	public int read() throws IOException {
		if (start) {
			startCounter();
			start = false;
		}
		this.counter--;
		return super.read();
	}

	public int read(byte b[], int off, int len) throws IOException {
		if (start) {
			startCounter();
			start = false;
		}
		counter -= len;
		return super.read(b, off, len);
	}

	public boolean exhausted() {
		return !start && counter == 0;
	}

}

