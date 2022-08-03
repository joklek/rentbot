package com.joklek.rentbot.entities;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Long telegramId;
    @NotNull
    private Boolean enabled;
    @Min(0)
    private Integer priceMin;
    @Min(0)
    private Integer priceMax;
    @Min(0)
    private Integer roomsMin;
    @Min(0)
    private Integer roomsMax;
    @Min(0)
    private Integer yearMin;
    @Min(0)
    private Integer floorMin;
    @NotNull
    private Boolean showWithFees;
    //    @NotNull
    private Boolean filterByDistrict;

    public User(Long id) {
        this.id = id;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public User setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
        return this;
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

    public Boolean getShowWithFees() {
        return showWithFees;
    }

    public void setShowWithFees(boolean showWithFees) {
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
