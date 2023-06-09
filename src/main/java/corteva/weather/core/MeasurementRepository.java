package corteva.weather.core;

import org.springframework.data.domain.*;
import org.springframework.data.repository.*;

import java.util.*;

/**
 * handles persistence & paging + sorting,
 * just like it says on the tin.
 */
public interface MeasurementRepository extends PagingAndSortingRepository<Measurement, MeasurementId>, ListCrudRepository<Measurement, MeasurementId> {

    List<Measurement> findAllByStation(String station);

    <T> Page<Measurement> findAll(Example<T> of, Pageable paging);

}
