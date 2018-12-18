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

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class VivoFit3Constants {
    public static final UUID UUID_SERVICE_VIVOFIT3 = UUID.fromString("9b012401-bc30-ce9a-e111-0f67e491abde");
    public static final UUID UUID_SERVICE_VIVOFIT3_WRITER = UUID.fromString("df334c80-e6a7-d082-274d-78fc66f85e16");
    public static final UUID UUID_SERVICE_VIVOFIT3_READER = UUID.fromString("4acbcd28-7425-868e-f447-915c8f00d0cb");
}
