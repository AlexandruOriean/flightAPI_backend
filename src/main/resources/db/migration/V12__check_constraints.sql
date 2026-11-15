-- The database enforces the rules the API validates (FlightRequest, AirplaneRequest), so no write
-- path - a future code path, a script, a manual fix - can store a flight that lands before it
-- leaves or an airplane without seats. Fails on existing rows that break a rule: fix them first.

ALTER TABLE flight ADD CONSTRAINT ck_flight_arrival_after_departure CHECK (arrival_at > departure_at);
ALTER TABLE flight ADD CONSTRAINT ck_flight_different_airports CHECK (departure_airport_id <> arrival_airport_id);
ALTER TABLE flight ADD CONSTRAINT ck_flight_price_not_negative CHECK (price_amount >= 0);

ALTER TABLE airplane ADD CONSTRAINT ck_airplane_seats_positive CHECK (number_of_seats > 0);
ALTER TABLE airplane ADD CONSTRAINT ck_airplane_speed_positive CHECK (speed > 0);
