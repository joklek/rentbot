package com.joklek.rentbot.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;


@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private Boolean enabled;
    private Integer priceMin;
    private Integer priceMax;
    private Integer roomsMin;
    private Integer roomsMax;
    private Integer yearMin;
    private Integer floorMin;
    private Integer showWithFees;
    private Boolean filterByDistrict;

    public User(Long id) {
        this.id = id;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Integer priceFrom) {
        this.priceMin = priceFrom;
    }

    public Integer getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Integer priceTo) {
        this.priceMax = priceTo;
    }

    public Integer getRoomsMin() {
        return roomsMin;
    }

    public void setRoomsMin(Integer roomsFrom) {
        this.roomsMin = roomsFrom;
    }

    public Integer getRoomsMax() {
        return roomsMax;
    }

    public void setRoomsMax(Integer roomsTo) {
        this.roomsMax = roomsTo;
    }

    public Integer getYearMin() {
        return yearMin;
    }

    public void setYearMin(Integer yearFrom) {
        this.yearMin = yearFrom;
    }

    public Integer getFloorMin() {
        return floorMin;
    }

    public void setFloorMin(Integer minFloor) {
        this.floorMin = minFloor;
    }

    public Integer getShowWithFees() {
        return showWithFees;
    }

    public void setShowWithFees(Integer showWithFees) {
        this.showWithFees = showWithFees;
    }

    public Boolean getFilterByDistrict() {
        return filterByDistrict;
    }

    public void setFilterByDistrict(Boolean filterByDistrict) {
        this.filterByDistrict = filterByDistrict;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return this.id != null &&
                Objects.equals(this.id, user.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
