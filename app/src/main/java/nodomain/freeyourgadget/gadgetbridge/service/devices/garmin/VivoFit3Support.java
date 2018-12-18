/*  Copyright (C) 2020 Marco Sirabella

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
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.VivoFit3Constants;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class VivoFit3Support extends AbstractBTLEDeviceSupport {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Support.class);

	public VivoFit3Support() {
		super(LOG);
		addSupportedService(VivoFit3Constants.UUID_SERVICE_VIVOFIT3);
	}

	@Override
	public boolean connectFirstTime() {
		return super.connect();
	}

	@Override
	protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
		setInitialized(builder);
		return builder;
	}

	private void setInitialized(TransactionBuilder builder) {
		builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
	}

	@Override
	public boolean useAutoConnect() {
		return true; // false ?
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

	@Override
	public void onSendWeather(WeatherSpec weatherSpec) { /* nothing */ }

	@Override
	public void onSendConfiguration(String config) { /* nothing */ }
	@Override
	public void onReadConfiguration(String config) { /* nothing */ }


	@Override
	public void onReset(int flags) { /* nothing */ }




	@Override
	public void onNotification(NotificationSpec notificationSpec) { /* nothing */ }
	@Override
	public void onDeleteNotification(int id) { /* nothing */ }
	@Override
	public void onSetTime() { /* nothing */ }
	@Override
	public void onSetCallState(CallSpec callSpec) { /* nothing */ }
	@Override
	public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) { /* nothing */ }
	@Override
	public void onSetMusicState(MusicStateSpec stateSpec) { /* nothing */ }
	@Override
	public void onSetMusicInfo(MusicSpec musicSpec) { /* nothing */ }
	@Override
	public void onEnableRealtimeSteps(boolean enable) { /* nothing */ }
	@Override
	public void onAppInfoReq() { /* nothing */ }
	@Override
	public void onAppStart(UUID uuid, boolean start) { /* nothing */ }
	@Override
	public void onAppDelete(UUID uuid) { /* nothing */ }
	@Override
	public void onAppReorder(UUID[] uuids) { /* nothing */ }
	@Override
	public void onFetchRecordedData(int dataTypes) { /* nothing */ }
	@Override
	public void onEnableRealtimeHeartRateMeasurement(boolean enable) { /* nothing */ }
	@Override
	public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) { /* nothing */ }
	@Override
	public void onDeleteCalendarEvent(byte type, long id) { /* nothing */ }
	@Override
	public void onFindDevice(boolean start) { /* nothing */ }
	@Override
	public void onScreenshotReq() { /* nothing */ }
	@Override
	public void onEnableHeartRateSleepSupport(boolean enable) { /* nothing */ }
	@Override
	public void onTestNewFunction() { /* nothing */ }
	@Override
	public void onSetConstantVibration(int intensity) { /* nothing */ }
	@Override
	public void onSetHeartRateMeasurementInterval(int seconds) { /* nothing */ }
	@Override
	public void onHeartRateTest() { /* nothing */ }
	@Override
	public void onSetAlarms(ArrayList<? extends Alarm> alarms) { /* nothing */ }
}
