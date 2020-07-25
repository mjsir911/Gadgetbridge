/*  Copyright (C) 2020 Marco Sirabella, mormegil

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.util.zip.Checksum;
import java.io.BufferedInputStream;
import java.io.IOException;


class CRC16 implements Checksum {  
	public static final class IBM extends CRC16 {
		static {
			table = new short[] {(short) 0x0000, (short) 0xcc01, (short) 0xd801, (short) 0x1400, (short) 0xf001, (short) 0x3c00, (short) 0x2800, (short) 0xe401, (short) 0xa001, (short) 0x6c00, (short) 0x7800, (short) 0xb401, (short) 0x5000, (short) 0x9c01, (short) 0x8801, (short) 0x4400};
		}
		public IBM() {
			super(0x8005, 0x0000);
		}
	}
	public static final class XMODEM extends CRC16 {
		public XMODEM() {
			super(0x1021, 0x0000);
		}
	}

	private short crc;


	private final int polynomial;
	private final int start;
	protected static short[] table;

	public CRC16(int polynomial, int start) {
		this.polynomial = polynomial;
		this.start = start;
		reset();
	}

	public void reset() {
		crc = (short) start;
	}

	public void update(int b) {
		// b is actually a byte
		crc = (short) ((((crc >> 4) & 0xFFF) ^ table[crc & 0xF]) ^ table[b & 0xF]);
		crc = (short) ((((crc >> 4) & 0xFFF) ^ table[crc & 0xF]) ^ table[(b >> 4) & 0xF]);
	}

	public void update(byte[] b, int off, int len) {
		for (int i = off; i < off + len; i++) {
			update(b[i]);
		}
	}
	public long getValue() {
		return crc & 0xFFFF;
	}

	public static void main(String args[]) {
		CRC16 crc = new CRC16.IBM();

		try (BufferedInputStream in = new BufferedInputStream(System.in)) {
			while (in.available() != 0) {
				crc.update(in.read());
			}
		} catch (IOException e) {}
		System.out.println(Long.toHexString(crc.getValue()));
	}
}
