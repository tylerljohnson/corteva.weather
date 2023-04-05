package corteva.weather.rest;

import corteva.weather.core.*;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

@RestController
@RequestMapping("api/weather/stats")
@Validated
@Slf4j
public class StatsController {
    private static final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy");

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false, defaultValue = "", name = "station") String station,
            @RequestParam(required = false, defaultValue = "0", name = "year") @Min(0) @Max(9999) int yearInt,
            @RequestParam(required = false, defaultValue = "0", name = "page") @Min(0) @Max(Integer.MAX_VALUE) int pageNum,
            @RequestParam(required = false, defaultValue = "5", name = "size") @Min(1) @Min(1) @Max(100) int pageSize
    ) {
        log.info(String.format(
                "REST: Stats - findAll() - station=%s, year=%d, page=%d, size=%d",
                station, yearInt, pageNum, pageSize
        ));

        LocalDate localYear = null;
        if (yearInt != 0) {
            localYear = LocalDate.parse(String.format("%s-01-01", yearInt), inputFormat);
        }

        Stats.StatsBuilder builder = Stats.builder();
        if ("".equals(station)) station = null;
        if (station != null) {
            builder.station(station);
        }
        if (localYear != null) {
            builder.year(localYear);
        }
        Stats example = builder.build();

        Pageable paging = PageRequest.of(pageNum, pageSize);
        Page<Stats> page = service.findAll(example, paging);

        List<Stats> statsList = page.getContent();

        Map<String, Object> response = new HashMap<>();
        response.put("stats", statsList);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("pageSize", pageSize);
        response.put("station", station);
        response.put("year", localYear == null ? null : outputFormat.format(localYear));

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