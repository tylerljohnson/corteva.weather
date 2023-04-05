package corteva.weather.rest;

import corteva.weather.core.*;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * rest controller for Measurements.
 * Validation and paging/filtering is handled
 * here as well.
 */
@RestController
@RequestMapping("api/weather")
@Validated
@Slf4j
public class MeasurementController {
    private static final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MeasurementService service;

    public MeasurementController(MeasurementService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false, name = "station") String station,
            @RequestParam(required = false, name = "date") @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "0", name = "page") @Valid @Min(0) @Max(Integer.MAX_VALUE) int pageNum,
            @RequestParam(required = false, defaultValue = "5", name = "size") @Valid @Min(1) @Min(1) @Max(100) int pageSize
    ) {
        log.info(String.format(
                "REST: Measurement - findAll() - station=%s, date=%s, page=%d, size=%d",
                station, (date != null ? outputFormatter.format(date) : "null"), pageNum, pageSize
        ));

        Measurement.MeasurementBuilder builder = Measurement.builder();
        if ("".equals(station)) station = null;
        if (station != null) {
            builder.station(station);
        }
        if (date != null) {
            builder.date(date);
        }
        Measurement example = builder.build();

        Pageable paging = PageRequest.of(pageNum, pageSize);
        Page<Measurement> page = service.findAll(example, paging);

        List<Measurement> statsList = page.getContent();

        Map<String, Object> response = new HashMap<>();
        response.put("stats", statsList);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("station", station);
        response.put("date", date == null ? null : outputFormatter.format(date));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return errors;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ConstraintViolationException.class})
    public Map<String, String> handleConstraintValidationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(error -> errors.put(error.getPropertyPath().toString(), error.getMessage()));

        return errors;
    }

}