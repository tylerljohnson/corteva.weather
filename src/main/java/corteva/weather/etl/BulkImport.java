package corteva.weather.etl;

import corteva.weather.core.*;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.time.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

/**
 * Perform a bulk insert of WeatherData objects into the database.
 * Will scan a directory  for txt files, processing each of them.
 * Duplicate items will not be imported.
 * Station & date must be unique.
 * Summarization can be triggered here as well, which will re-compute
 * all the stats bases on the current state.
 */
@Service
@Slf4j
public class BulkImport {
    public static final int ONLY_CURRENT_DIRECTORY = 1;
    private static final String NULL_TOKEN = "-9999";
    private static final DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final MeasurementService weatherDataService;
    private final StatsRepository statsRepository;
    private long totalInserted = 0; // how many inserts were done
    private long fileInserted = 0; // how many inserts from the current file
    private long fileRead = 0;  // home many lines did we read in the current file
    private List<LocalDate> currentStationDates;
    private List<Measurement> itemsToInsert; // list of models from the current file to be inserted
    @Value("${bulk.import.dir}")
    private Path bulkImportDir; // dir where we expect the import files to live

    @Value("${bulk.import.enabled}")
    private boolean enabled; // dir where we expect the import files to live

    public BulkImport(MeasurementService weatherDataService, StatsRepository statsRepository) {
        this.weatherDataService = weatherDataService;
        this.statsRepository = statsRepository;
    }

    /**
     * Main entry point, start the import process
     */
    public void startImport() {
        if (!enabled) {
            log.info("bulk import disabled");
            return;
        }

        log.info("started");
        StopWatch totalStopWatch = StopWatch.createStarted();
        try {
            if (bulkImportDir == null) {
                log.error("importPath cannot be null");
                return;
            }
            if (!bulkImportDir.toFile().exists()) {
                log.error("importPath must exist");
                return;
            }

            if (!bulkImportDir.toFile().isDirectory()) {
                log.error("importPath must be a directory");
                return;
            }

            importFromDirectory(bulkImportDir);
        } catch (Throwable t) {
            log.error("import failed", t);
        } finally {
            totalStopWatch.stop();
            log.info(String.format(
                    "complete: inserted %,d record%s, elapsed time: %s",
                    totalInserted, (totalInserted != ONLY_CURRENT_DIRECTORY ? "s" : ""), totalStopWatch.formatTime()
            ));
        }
    }

    // scan the given directory and process all *.txt from it.
    private void importFromDirectory(Path aDir) throws IOException {
        Collection<Path> inputFiles = scanForFilesToImport(aDir);

        int idx = 0;
        int totalFilesToImport = inputFiles.size();
        log.debug(String.format("%,d files found", totalFilesToImport));
        totalInserted = 0;
        for (Path currentPath : inputFiles) {
            StopWatch fileStopWatch = StopWatch.createStarted();
            fileInserted = 0;
            fileRead = 0;
            idx++;

            importFile(currentPath);

            fileStopWatch.stop();
            log.info(String.format(
                    "%3d/%-3d %s : read %,d\tinserted %,d\titems/sec %,.0f",
                    idx, totalFilesToImport,
                    currentPath.getFileName(),
                    fileRead,
                    fileInserted,
                    ((double) fileRead / (double) fileStopWatch.getTime()) * 1000.0
            ));
        }
    }

    // read lines from the given file
    private void importFile(Path path) throws IOException {
        log.debug(String.format("import from file: %s", path.toString()));
        // reset/empty the list of models to store in db
        itemsToInsert = new ArrayList<>();

        String station = toStation(path);
        if (station == null) {
            log.warn("station name was null");
            return;
        }
        log.debug(String.format("station=%s", station));

        currentStationDates = lookupDatesForStation(station);
        log.debug(String.format("%,d initial existing dates for station %s", currentStationDates.size(), station));

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile()))) {
            for (String line; (line = bufferedReader.readLine()) != null; ) {
                fileRead++;
                processLine(station, line);
            }

            log.debug(String.format("storing %,d items", itemsToInsert.size()));
            weatherDataService.createAll(itemsToInsert);
        }
    }

    // convert the String into a model and store it
    private void processLine(String station, String line) {
        Measurement model = toModel(line);
        if (model == null) {
            log.debug("model was null");
            return;
        }

        // station value wasn't in the file, it is derived from the filename
        model.setStation(station);
        addToCreateList(model);
    }

    // fetch all the current date values for the station from the db
    private List<LocalDate> lookupDatesForStation(String station) {
        return weatherDataService
                .findAllByStation(station)
                .stream()
                .map(Measurement::getDate)
                .collect(Collectors.toList());
    }

    // strip the ".txt" file extension off of the path's filename to determine the station value
    private String toStation(Path p) {
        String filename = p.getFileName().toString();

        int idx = filename.lastIndexOf(".");
        if (idx < 0) {
            log.debug("filename has no extension");
            return null;
        }

        return filename.substring(0, idx);
    }

    // convert the String line into a model
    private Measurement toModel(String line) {
        if (line == null) {
            log.debug("line was null");
            return null;
        }
        if (line.length() < ONLY_CURRENT_DIRECTORY) {
            log.debug("line was empty");
            return null;
        }

        String[] parts = line.split("\t");
        if (parts == null) {
            log.debug("line had no delimited parts");
            return null;
        }
        if (parts.length != 4) {
            log.debug("line had wrong number of delimited parts");
            return null;
        }

        Measurement model = new Measurement();

        model.setDate(toLocalDate(parts[0]));
        model.setMaxTemp(toInt(parts[ONLY_CURRENT_DIRECTORY]));
        model.setMinTemp(toInt(parts[2]));
        model.setTotalPrecip(toInt(parts[3]));

        if (log.isTraceEnabled()) {
            log.trace(String.format("line text[%s] resulted in model [%s]", line, model));
        }

        return model;
    }

    // should we add the model to the list for later persistence
    private void addToCreateList(Measurement model) {
        boolean exists = currentStationDates.contains(model.getDate());
        if (!exists) {
            itemsToInsert.add(model);  // list to save later
            currentStationDates.add(model.getDate()); // save this date to prevent dupes
            totalInserted++;
            fileInserted++;
        }
    }

    // convert a string in temperature format, into an Integer
    private Integer toInt(String s) {
        if (s == null) return null;
        s = s.trim();
        if (hasNullToken(s)) return null;
        return Integer.parseInt(s);
    }

    // some fields represent a null with a specific string value
    private boolean hasNullToken(String s) {
        return NULL_TOKEN.equals(s);
    }

    // convert a string to a LocalDate
    private LocalDate toLocalDate(String s) {
        if (s == null) return null;
        s = s.trim();
        return LocalDate.parse(s, localDateFormatter);
    }

    // find txt files in the given directory that are a readable, a directory and ends in ".txt"
    private Collection<Path> scanForFilesToImport(Path path) throws IOException {
        return Files.walk(path, ONLY_CURRENT_DIRECTORY)
                .filter(Files::isRegularFile)
                .filter(Files::isReadable)
                .filter(f -> hasExtension(f, "txt"))
                .collect(Collectors.toList());
    }

    // grab the file extension (any text after the last . in the filename)
    private boolean hasExtension(Path path, String extension) {
        return path.getFileName().toString().endsWith(extension);
    }

    public void summarize() {
        if (!enabled) return;

        log.info("summarize started");
        statsRepository.deleteAll();
        statsRepository.summarizeAll();
        log.info("summarize complete");
    }
}