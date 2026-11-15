-- Flight search filters on the departure time; PostgreSQL does not index foreign keys by itself,
-- so joins to airports/airlines and the FK checks on deleting them would scan the whole table.

CREATE INDEX idx_flight_departure_at ON flight (departure_at);
CREATE INDEX idx_flight_departure_airport ON flight (departure_airport_id);
CREATE INDEX idx_flight_arrival_airport ON flight (arrival_airport_id);
CREATE INDEX idx_flight_airline ON flight (airline_id);

CREATE INDEX idx_airport_city ON airport (city_id);
