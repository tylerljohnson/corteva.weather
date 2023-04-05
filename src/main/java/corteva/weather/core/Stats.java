package corteva.weather.core;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.*;

import java.io.*;
import java.time.*;

@Entity
@Data
@Table(name = "stats")
@IdClass(StatsId.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stats implements Serializable {
    @Size(min = 1, max = 255)
    @Id
    @NotNull
    private String station;
    @Id
    @NotNull
    @DateTimeFormat(pattern = "yyyy")
    @JsonFormat(pattern = "yyyy")
    private LocalDate year;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Float avgMinTemp;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Float avgMaxTemp;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Float avgPrecip;

    public Stats(String station, LocalDate year) {
        this.station = station;
        this.year = year;
    }
}
