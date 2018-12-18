package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.OutputStream;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nodomain.freeyourgadget.gadgetbridge.Logging;

class VivoFit3OutputStream extends OutputStream {
private static final Logger LOG = LoggerFactory.getLogger(VivoFit3OutputStream.class);
	private int mOffset = 0;
	private String fpath;
	private byte subtype;
	private VivoFit3IoThread writer;

	private short mfIndex;

	public VivoFit3OutputStream(String fpath, byte subtype, VivoFit3IoThread writer) {
		super();
		this.fpath = fpath;
		this.subtype = subtype;
		this.writer = writer;

	}

	public void flush() {
		synchronized (this) {
			try {
				this.wait();
			} catch (InterruptedException e) {}
		}
	}

	private void finishWrite() {
		synchronized(this) {
			this.notify();
		}
	}

	public void write(@NonNull final byte[] b) {
		LOG.debug("start write");
		final VivoFit3OutputStream that = this;
		writer.writeWithResponseCallback(createFile(fpath, b.length, subtype, System.currentTimeMillis()), 
			new VivoFit3IoThread.MyRunnable() {
				public void run(byte[] resp) {
					LOG.debug("#################from write create file response : " + Logging.formatBytes(resp));
					// order of these matter
					ByteBuffer bb = ByteBuffer.wrap(resp).order(ByteOrder.LITTLE_ENDIAN);
					byte respStatus = bb.get();
					if (!(respStatus == 0x00)) {
						LOG.error("response status bad: " + respStatus);
						return;
					}
					short fIndex = bb.getShort();
					//byte fDataType = bb.get();
					//byte fDataSubType = bb.get();
					//short fNumber = bb.getShort();
					mfIndex = fIndex;
					writer.writeWithResponseCallback(encodeUploadRequest(mfIndex, b.length, mOffset),
						new VivoFit3IoThread.MyRunnable() {
							public void run(byte[] resp) {
								LOG.debug("#################from upload response : " + Logging.formatBytes(resp));
								writer.writeWithResponseCallback(encodeFileTransfer(b, mOffset),
									new VivoFit3IoThread.MyRunnable() {
										public void run(byte[] resp) {
											LOG.debug("#################from file transfer response : " + Logging.formatBytes(resp));
											mOffset += b.length;
											finishWrite();
										}
									}
								);
							}
						}
					);
				}
			}
		);
	}


	public void write(int b) {
		write(new byte[] {(byte) b});
	}
	public void write(byte[] b, int off, int len) {
		byte[] actual = new byte[len];
		System.arraycopy(b, off, actual, 0, len);
		write(actual);
	}

	static private byte[] createFile(String fpath, int fsize, byte subtype, long bigid) {
		/*
			 0                   1                   2                   3
			 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |          Full Length          |       Message Identifier      |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |                           File Size                           |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |   File Type   |  File Subtype |      File Data Identifier     |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |    RESERVED   |  Subtype Mask |        File Number Mask       |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |          Path Length          |   Variable Length File Path   |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |                               |                               |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
			 |                      Big File Identifier                      |
			 +                               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |                               |              CRC              |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


			 full length is not added here
			 */
		return ByteBuffer
			.allocate(24 + fpath.length())
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short)  0x138d) // message id
			.putInt(  (int)    fsize)  // file size
			.put(     (byte)   0xff)   // file type
			.put(     (byte)   subtype)   // file subtype
			.putShort((short) 0x0000) // file data id
			.put(     (byte)   0x00)   // reserved
			.put(     (byte)   0x00)   // subtype mask
			.putShort((short)  0xffff) // file number mask
			.putShort((short) fpath.length()) // file path length
			.put(fpath.getBytes())
			.putLong( (long)   bigid) // big identifier
			.array();

		// response consists of:
		// response ( 1 byte)
		//	enum:
		//		file created successfully (0)
		//		file already exists (1)
		//		not enough space (2)
		//		not supported (3)
		//		no slots available for file type (4)
		//		not enough space for file type (5)
		// file index (2 bytes)
		// file data type ( 1 byte)
		// file data subtype (1 byte)
		// file number ( 2 bytes)
	}

	static private byte[] encodeUploadRequest(short fIndex, int maxSize, int offset) {
		/*
			 0                   1
			 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |             Length            |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |           Message Id          |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |           File Index          |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |                               |
			 +            Max Size           +
			 |                               |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |                               |
			 +          Data Offset          +
			 |                               |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |            CRC Seed           |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 |              CRC              |
			 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			 */
		return ByteBuffer
			.allocate(14)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0x138b) // message id
			.putShort(fIndex) // file index
			.putInt(maxSize) // max size
			.putInt(offset) // data offset
			.putShort((short) 0x00) // crc seed
			.array();

		/* response message:
		 * 	response (1)
		 * 	data offset (4)
		 * 	max file size (4)
		 * 	crc seed (2)
		 */

	}

	static private byte[] encodeFileTransfer(byte[] data, int offset) {
		short flags = (byte) 0x00;
		short crc = (short) 0x000; // they don't actually check the CRC either
		return ByteBuffer
			.allocate(9 + data.length)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0x138c)
			.put(     (byte) flags) // flags
			.putShort((short) crc) // file crc
			.putInt(  (int) offset) // file offset
			.put(data)
			.array();

	}
}

