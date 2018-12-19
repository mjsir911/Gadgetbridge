/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
		Gobbetti, Kasha, Steffen Liebergeld

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

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;

import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public class VivoFit3Support extends AbstractSerialDeviceSupport {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Support.class);
	protected boolean firstTime = false;

	@Override
	public boolean connect() {
		LOG.debug("___________MARCO connect________________");
		getDeviceIOThread().start();
		return true;
	}

	@Override
	public boolean connectFirstTime() {
		LOG.debug("___________MARCO connectfirsttime________________");
		firstTime = true;
		return connect();
	}

	@Override
	protected GBDeviceProtocol createDeviceProtocol() {
		LOG.debug("___________MARCO createdeviceprotocol________________");
		return new VivoFit3Protocol(getDevice());
	}

	@Override
	protected GBDeviceIoThread createDeviceIOThread() {
		LOG.debug("___________MARCO createdeviceIOthread___________________");
		return new VivoFit3IoThread(VivoFit3Support.this, getDevice(), (VivoFit3Protocol) getDeviceProtocol(), getBluetoothAdapter(), getContext());
	}

	@Override
	public boolean useAutoConnect() {
		return true;
	}

	@Override
	public void onAppConfiguration(UUID uuid, String config, Integer id) {
		LOG.debug(config);
		// TODO: do this
	}

	public void onInstallApp(Uri uri) {
		// TODO: do this
		LOG.debug(uri.toString());
	}

	/**
	 * Sends the given message to the device. This implementation delegates the
	 * writing to the {@link #getDeviceIOThread device io thread}
	 *
	 * @param bytes the message to send to the device
	 */
	private void sendToDevice(byte[] bytes) {
		if (bytes != null && gbDeviceIOThread != null) {
			gbDeviceIOThread.write(bytes);
		}
	}

	public void onSyncComplete() {
		byte[] bytes = ((VivoFit3Protocol) getDeviceProtocol()).encodeSyncComplete();
		sendToDevice(bytes);
	}

	public void onPairStart() {
		byte[] bytes = ((VivoFit3Protocol) getDeviceProtocol()).encodePairStart();
		sendToDevice(bytes);
	}

	public void onPairComplete() {
		byte[] bytes = ((VivoFit3Protocol) getDeviceProtocol()).encodePairComplete();
		sendToDevice(bytes);
	}


	@Override
	public void onSetConstantVibration(int intensity) {} // nothing
	@Override
	public void onSetHeartRateMeasurementInterval(int seconds) {} // nothing
	@Override
	public void onHeartRateTest() {} // nothing
	@Override
	public void onSetAlarms(ArrayList<? extends Alarm> alarms) {} // nothing
}
