package corteva.weather.core;

import org.springframework.data.domain.*;
import org.springframework.stereotype.*;

import java.util.*;

/**
 * Business logic goes here
 */
@Service
public class MeasurementService {

    final MeasurementRepository repository;

    public MeasurementService(MeasurementRepository repository) {
        this.repository = repository;
    }

    public List<Measurement> createAll(List<Measurement> list) {
        return repository.saveAll(list);
    }

    public List<Measurement> findAllByStation(String station) {
        return repository.findAllByStation(station);
    }

    public Page<Measurement> findAll(Measurement example, Pageable paging) {
        return repository.findAll(Example.of(example), paging);
    }

}
