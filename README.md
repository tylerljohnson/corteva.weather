# corteva.weather

Corteva skills assessment.

Tools used: Java 17, Springboot (jpa, web, validation, springdoc-openapi), mysql, docker.

The tooling used is common in the modern Java ecosystems, which is why I chose it.

All output is for this app is done via console logging.

## Answers

### Problem 1 - Data Modeling

[corteva.weather.core.Measurements](src/main/java/corteva/weather/core/Measurement.java) models the imported data. I chose to leave the temps and precipitation as `Integer` values. This could easily be stored as a `Float`. Since the api only had a single GET endpoint, I choose to use `station` and `date` as a composite key.

### Problem 2 - Ingestion

[corteva.weather.BulkImport](src/main/java/corteva/weather/etl/BulkImport.java) does all the ingestion work. It's packaged on its own to facilitate breaking this monolith into separate modules, should that be needed.

### Problem 3 - Data Analysis

[corteva.weather.core.Stats](src/main/java/corteva/weather/core/Stats.java) models the summary data. A composite key of `station` and `year` was again used for the same reason as `Measurement`.

The last stage of [BulkImport](src/main/java/corteva/weather/etl/BulkImport.java) triggers the summarization process. [StatsRepository.summarizeAll()](src/main/java/corteva/weather/core/StatsRepository.java) method summarizes the data using sql in the db.

### Problem 4 - REST API

[MeasurementController](src/main/java/corteva/weather/rest/MeasurementController.java) & [StatsController](src/main/java/corteva/weather/rest/StatsController.java) handle the two `GET` endpoints and associated filtering & paging.

Validation of request params and models are handled there as well.

Api docs: [swagger ui](http://localhost/swagger) & [swagger.json](http://localhost/api-docs)

Example api calls with all request params present:

- [/api/weather?page=1&size=5&station=USC00336196&date=1999-12-31](http://localhost/api/weather?page=1&size=5&station=USC00336196&date=1999-12-31)
- [/api/weather/stats?page=1&size=5&station=USC00336196&year=1999](http://localhost/api/weather/stats?page=1&size=5&station=USC00336196&year=1999)

## Requirements

You'll need two things:

- [Docker](https://www.docker.com/products/docker-desktop/) (this uses docker compose)
- Java 17

### how to build the app

To build the app, just run `./mvnw clean install` or `mvnw.cmd clean install` if you are on Windows.

### starting the docker image

`docker compose image up`

You'll see `READY` in the log output once the web server is fully ready.

### stopping the container

Hit `Ctr==-C` to exit the container, then `docker compose down`

### How to enable/disable etl on startup

Edit [docker-compose.yml](docker-compose.yml) & adjust the values for`bulk_import_enabled`. Also, adjust where the `/wx-data`directory lives on your machine, this is in the`Volumes` section.

Once the import is complete you can change the `bulk_import_enabled` back to `false` to speed up the container start time.
