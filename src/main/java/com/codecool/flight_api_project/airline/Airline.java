package com.codecool.flight_api_project.airline;

import javax.persistence.*;

@Entity
@Table(name="Airline")
public class Airline
{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;




    public Airline(String name)
    {
        this.name = name;
    }

    public Airline()
    {
    }

    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "AirlineModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
