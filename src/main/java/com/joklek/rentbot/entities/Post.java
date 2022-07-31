package com.joklek.rentbot.entities;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private LocalDateTime lastSeen;
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

    public String getPhone() {
        return phone;
    }

    public Post setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public Integer getConstructionYear() {
        return constructionYear;
    }

    public void setConstructionYear(Integer year) {
        this.constructionYear = year;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Integer getTotalFloors() {
        return totalFloors;
    }

    public void setTotalFloors(Integer totalFloors) {
        this.totalFloors = totalFloors;
    }

    public Boolean getWithFees() {
        return isWithFees;
    }

    public void setWithFees(Boolean withFees) {
        isWithFees = withFees;
    }

    public String getDescriptionHash() {
        return descriptionHash;
    }

    public void setDescriptionHash(String descriptionHash) {
        this.descriptionHash = descriptionHash;
    }

    public String getStreet() {
        return street;
    }

    public Post setStreet(String street) {
        this.street = street;
        return this;
    }

    public String getDistrict() {
        return district;
    }

    public Post setDistrict(String district) {
        this.district = district;
        return this;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public Post setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
        return this;
    }

    public String getHeating() {
        return heating;
    }

    public Post setHeating(String heating) {
        this.heating = heating;
        return this;
    }

    public BigDecimal getArea() {
        return area;
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
