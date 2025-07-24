package ru.terra.mosaic.util;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Date: 21.07.15
 * Time: 16:32
 */
@AllArgsConstructor
@NoArgsConstructor
public class AvgColor implements Cloneable {
    public Float r = (float) 0;
    public Float g = (float) 0;
    public Float b = (float) 0;
    public Float a = (float) 0;

    public AvgColor cl() {
        return new AvgColor(r, g, b, a);
    }
}
