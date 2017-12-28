/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.vtype;

import java.text.NumberFormat;
import org.epics.util.stats.Range;

/**
 * Immutable Display implementation.
 *
 * @author carcassi
 */
class IDisplay extends Display {

    private final Range displayRange;
    private final Range warningRange;
    private final Range alarmRange;
    private final Range controlRange;
    private final String unit;
    private final NumberFormat format;

    public IDisplay(Range displayRange, Range warningRange, Range alarmRange,
            Range controlRange, String unit, NumberFormat format) {
        VType.argumentNotNull("displayRange", displayRange);
        VType.argumentNotNull("warningRange", warningRange);
        VType.argumentNotNull("alarmRange", alarmRange);
        VType.argumentNotNull("controlRange", controlRange);
        VType.argumentNotNull("unit", unit);
        VType.argumentNotNull("format", format);
        this.displayRange = displayRange;
        this.warningRange = warningRange;
        this.alarmRange = alarmRange;
        this.controlRange = controlRange;
        this.unit = unit;
        this.format = format;
    }

    @Override
    public Range getDisplayRange() {
        return displayRange;
    }

    @Override
    public Range getWarningRange() {
        return warningRange;
    }

    @Override
    public Range getAlarmRange() {
        return alarmRange;
    }

    @Override
    public Range getControlRange() {
        return controlRange;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public NumberFormat getFormat() {
        return format;
    }

}
