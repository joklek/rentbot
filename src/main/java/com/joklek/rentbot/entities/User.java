package com.joklek.rentbot.entities;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private long telegramId;
    @NotNull
    private boolean enabled;
    @NotNull
    @Min(0)
    @Max(100000)
    private BigDecimal priceMin;
    @NotNull
    @Min(0)
    @Max(100000)
    private BigDecimal priceMax;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer roomsMin;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer roomsMax;
    @NotNull
    @Min(1000)
    @Max(3000)
    private Integer yearMin;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer floorMin;
    @NotNull
    private boolean showWithFees;
    //    @NotNull
    private boolean filterByDistrict;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "user_districts",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "district_id")
    )
    private Set<District> districts = new HashSet<>();

    public User(long telegramId) {
        this.telegramId = telegramId;
        this.enabled = false;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public User setTelegramId(long telegramId) {
        this.telegramId = telegramId;
        return this;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Optional<BigDecimal> getPriceMin() {
        return Optional.ofNullable(priceMin);
    }

    public void setPriceMin(BigDecimal priceFrom) {
        this.priceMin = priceFrom;
    }

    public Optional<BigDecimal> getPriceMax() {
        return Optional.ofNullable(priceMax);
    }

    public void setPriceMax(BigDecimal priceTo) {
        this.priceMax = priceTo;
    }

    public Optional<Integer> getRoomsMin() {
        return Optional.ofNullable(roomsMin);
    }

    public void setRoomsMin(Integer roomsFrom) {
        this.roomsMin = roomsFrom;
    }

    public Optional<Integer> getRoomsMax() {
        return Optional.ofNullable(roomsMax);
    }

    public void setRoomsMax(Integer roomsTo) {
        this.roomsMax = roomsTo;
    }

    public Optional<Integer> getYearMin() {
        return Optional.ofNullable(yearMin);
    }

    public void setYearMin(Integer yearFrom) {
        this.yearMin = yearFrom;
    }

    public Optional<Integer> getFloorMin() {
        return Optional.ofNullable(floorMin);
    }

    public void setFloorMin(Integer minFloor) {
        this.floorMin = minFloor;
    }

    public boolean getShowWithFees() {
        return showWithFees;
    }

    public void setShowWithFees(boolean showWithFees) {
        this.showWithFees = showWithFees;
    }

    public boolean getFilterByDistrict() {
        return filterByDistrict;
    }

    public void setFilterByDistrict(boolean filterByDistrict) {
        this.filterByDistrict = filterByDistrict;
    }

    public Set<District> getDistricts() {
        return districts;
    }

    public User setDistricts(Set<District> districts) {
        this.districts = districts;
        return this;
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
