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
    private LocalDateTime lastSeen;
    private BigDecimal price;
    private Integer rooms;
    private Integer year;
    private Integer floor;
    private Integer totalFloors;
    private Boolean isWithFees;
    private String address;
    private String descriptionHash;

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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescriptionHash() {
        return descriptionHash;
    }

    public void setDescriptionHash(String descriptionHash) {
        this.descriptionHash = descriptionHash;
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
