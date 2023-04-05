package corteva.weather.core;

import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;

@Service
@Slf4j
public class StatsService {
    private final StatsRepository repository;

    public StatsService(StatsRepository repository) {
        this.repository = repository;
    }

    public Page<Stats> findAll(Stats example, Pageable paging) {
        return repository.findAll(Example.of(example), paging);
    }
}
