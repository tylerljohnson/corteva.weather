package corteva.weather.core;

import lombok.*;

import java.io.*;
import java.time.*;

/**
 * The natural key for a measurement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsId implements Serializable {
    public String station;
    public LocalDate year;
}
