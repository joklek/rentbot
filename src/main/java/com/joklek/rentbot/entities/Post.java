package com.joklek.rentbot.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String externalId;
    private String source;
    private String link;
    private String phone;
    private LocalDateTime createdAt;
    private BigDecimal price;
    private Integer rooms;
    private Integer constructionYear;
    private Integer floor;
    private Integer totalFloors;
    private Boolean isWithFees;
    private String descriptionHash;
    private String street;
    private String district;
    private String houseNumber;
    private String heating;
    private BigDecimal area;

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public Post setExternalId(String internalId) {
        this.externalId = internalId;
        return this;
    }

    public String getSource() {
        return source;
    }

    public Post setSource(String source) {
        this.source = source;
        return this;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Post setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime lastSeen) {
        this.createdAt = lastSeen;
    }

    public Optional<BigDecimal> getPrice() {
        return Optional.ofNullable(price);
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Optional<Integer> getRooms() {
        return Optional.ofNullable(rooms);
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public Optional<Integer> getConstructionYear() {
        return Optional.ofNullable(constructionYear);
    }

    public void setConstructionYear(Integer year) {
        this.constructionYear = year;
    }

    public Optional<Integer> getFloor() {
        return Optional.ofNullable(floor);
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Optional<Integer> getTotalFloors() {
        return Optional.ofNullable(totalFloors);
    }

    public void setTotalFloors(Integer totalFloors) {
        this.totalFloors = totalFloors;
    }

    public boolean getWithFees() {
        return isWithFees;
    }

    public void setWithFees(boolean withFees) {
        isWithFees = withFees;
    }

    public Optional<String> getDescriptionHash() {
        return Optional.ofNullable(descriptionHash);
    }

    public void setDescriptionHash(String descriptionHash) {
        this.descriptionHash = descriptionHash;
    }

    public Optional<String> getStreet() {
        return Optional.ofNullable(street);
    }

    public Post setStreet(String street) {
        this.street = street;
        return this;
    }

    public Optional<String> getDistrict() {
        return Optional.ofNullable(district);
    }

    public Post setDistrict(String district) {
        this.district = district;
        return this;
    }

    public Optional<String> getHouseNumber() {
        return Optional.ofNullable(houseNumber);
    }

    public Post setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
        return this;
    }

    public Optional<String> getHeating() {
        return Optional.ofNullable(heating);
    }

    public Post setHeating(String heating) {
        this.heating = heating;
        return this;
    }

    public Optional<BigDecimal> getArea() {
        return Optional.ofNullable(area);
    }

    public Post setArea(BigDecimal area) {
        this.area = area;
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
        var post = (Post) o;
        return this.id != null &&
                Objects.equals(this.id, post.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
