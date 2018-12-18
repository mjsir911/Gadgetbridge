/*  Copyright (C) 2018 Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.VivoFit3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.VivoFit3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class VivoFit3SampleProvider extends AbstractSampleProvider<VivoFit3ActivitySample> {
    public VivoFit3SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<VivoFit3ActivitySample, ?> getSampleDao() {
        return getSession().getVivoFit3ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return VivoFit3ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return VivoFit3ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return VivoFit3ActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public VivoFit3ActivitySample createActivitySample() {
        return new VivoFit3ActivitySample();
    }
}
