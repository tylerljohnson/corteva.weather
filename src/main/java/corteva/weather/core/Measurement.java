package corteva.weather.core;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.*;

import java.io.*;
import java.time.*;

/**
 * Model the ingested data.
 * This doubles as the DTO since there is no artificial pk.
 * temps are in tenths of a C.
 * precip is in tenths of a mm.
 */
@Entity
@Data
@Table(name = "measurements")
@IdClass(MeasurementId.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Measurement implements Serializable {
    @Id
    @Size(min = 1, max = 255)
    @NotNull
    private String station;
    @Id
    @NotNull
    @Column(columnDefinition = "DATE")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Integer minTemp;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Integer maxTemp;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Integer totalPrecip;

    public Measurement(String station, LocalDate date) {
        this.station = station;
        this.date = date;
    }
}
