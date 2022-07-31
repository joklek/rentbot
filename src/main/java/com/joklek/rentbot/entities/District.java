package com.joklek.rentbot.entities;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "districts")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var district = (District) o;
        return this.id != null &&
                Objects.equals(this.id, district.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
