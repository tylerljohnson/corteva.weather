package corteva.weather;

import corteva.weather.etl.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Corteva Weather API", version = "1.0", description = "API for weather station data"))
@Slf4j
public class RestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }

    @Bean
    CommandLineRunner runBulkImport(BulkImport bulkImport) {
        return args -> {
            try {
                bulkImport.startImport();
                bulkImport.summarize();
            } finally {
                log.info("READY");
            }
        };
    }
}
