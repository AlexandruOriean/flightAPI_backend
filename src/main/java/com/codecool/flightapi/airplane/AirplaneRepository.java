package com.codecool.flightapi.airplane;

import org.springframework.data.jpa.repository.JpaRepository;

interface AirplaneRepository extends JpaRepository<Airplane, Long> {}
