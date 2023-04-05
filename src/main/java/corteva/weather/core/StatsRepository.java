package corteva.weather.core;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.*;
import org.springframework.transaction.annotation.*;

public interface StatsRepository extends PagingAndSortingRepository<Stats, StatsId>, ListCrudRepository<Stats, StatsId> {

    @Modifying
    @Query(value = "INSERT INTO stats " +
            "(station, year, avg_max_temp, avg_min_temp, avg_precip) " +
            "SELECT m.station, " +
            "MAKEDATE(YEAR(m.date), 1) AS year, " +
            "ROUND(AVG(CAST(m.max_temp AS FLOAT)) / 10.0, 1) AS avg_max_temp, " +
            "ROUND(AVG(CAST(m.min_temp AS FLOAT)) / 10.0, 1) AS avg_min_temp, " +
            "ROUND(AVG(CAST(m.total_precip AS FLOAT)) / 10.0 / 10.0, 2) AS avg_precip " +
            "FROM measurements m " +
            "GROUP BY m.station, MAKEDATE(YEAR(m.date), 1)", nativeQuery = true)
    @Transactional
    void summarizeAll();

    <T> Page<Stats> findAll(Example<T> of, Pageable paging);
}
