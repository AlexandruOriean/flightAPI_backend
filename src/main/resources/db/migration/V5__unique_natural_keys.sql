-- Create/update requests reference airports by IATA code, airlines by ICAO code (column `iso`)
-- and cities by name, so each of these must identify exactly one row.

ALTER TABLE airport ADD CONSTRAINT uk_airport_iata_code UNIQUE (airport_iata_code);
ALTER TABLE airline ADD CONSTRAINT uk_airline_iso UNIQUE (iso);
ALTER TABLE city ADD CONSTRAINT uk_city_name UNIQUE (city_name);
