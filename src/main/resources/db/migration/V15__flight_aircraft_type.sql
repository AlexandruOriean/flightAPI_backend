-- Flights may say which aircraft type operates them (optional: existing flights have no type).
-- An aircraft type cannot be deleted while flights reference it.

ALTER TABLE flight ADD COLUMN aircraft_type_id BIGINT;
ALTER TABLE flight ADD CONSTRAINT fk_flight_aircraft_type FOREIGN KEY (aircraft_type_id) REFERENCES aircraft_type (id);
CREATE INDEX idx_flight_aircraft_type ON flight (aircraft_type_id);

-- Types for the V2 seed flights; each update only touches the seed flight it was written for.
UPDATE flight SET aircraft_type_id = (SELECT id FROM aircraft_type WHERE icao_code = 'B738')
WHERE flight_id IN (1, 4) AND airline_id = (SELECT id FROM airline WHERE icao_code = 'ROT');
UPDATE flight SET aircraft_type_id = (SELECT id FROM aircraft_type WHERE icao_code = 'A320')
WHERE flight_id = 2 AND airline_id = (SELECT id FROM airline WHERE icao_code = 'BAW');
UPDATE flight SET aircraft_type_id = (SELECT id FROM aircraft_type WHERE icao_code = 'A320')
WHERE flight_id = 3 AND airline_id = (SELECT id FROM airline WHERE icao_code = 'AFR');
UPDATE flight SET aircraft_type_id = (SELECT id FROM aircraft_type WHERE icao_code = 'A21N')
WHERE flight_id = 5 AND airline_id = (SELECT id FROM airline WHERE icao_code = 'WZZ');
