-- airline.iso has always held the ICAO airline designator: name it accordingly.
ALTER TABLE airline RENAME COLUMN iso TO icao_code;
ALTER TABLE airline RENAME CONSTRAINT uk_airline_iso TO uk_airline_icao_code;

-- Columns the API requires (see the *Request records) must never be NULL.
ALTER TABLE city ALTER COLUMN city_name SET NOT NULL;

ALTER TABLE airport ALTER COLUMN airport_name SET NOT NULL;
ALTER TABLE airport ALTER COLUMN airport_iata_code SET NOT NULL;
ALTER TABLE airport ALTER COLUMN city_id SET NOT NULL;

ALTER TABLE airplane ALTER COLUMN manufacturer SET NOT NULL;
ALTER TABLE airplane ALTER COLUMN model SET NOT NULL;
ALTER TABLE airplane ALTER COLUMN number_of_seats SET NOT NULL;

ALTER TABLE flight ALTER COLUMN departure_date SET NOT NULL;
ALTER TABLE flight ALTER COLUMN departure_time SET NOT NULL;
ALTER TABLE flight ALTER COLUMN arrival_time SET NOT NULL;
ALTER TABLE flight ALTER COLUMN price SET NOT NULL;
ALTER TABLE flight ALTER COLUMN airline_id SET NOT NULL;
ALTER TABLE flight ALTER COLUMN departure_airport_id SET NOT NULL;
ALTER TABLE flight ALTER COLUMN arrival_airport_id SET NOT NULL;
