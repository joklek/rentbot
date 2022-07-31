package com.joklek.rentbot.scraper;

import java.math.BigDecimal;
import java.net.URI;

public abstract class PostDto {
    private String externalId;
    private URI link;
    private String phone;
    private String description;
    private String street;
    private String district;
    private String houseNumber;
    private String heating;
    private Integer floor;
    private Integer totalFloors;
    private Integer area;
    private BigDecimal price;
    private Integer rooms;
    private Integer year;

    public abstract String getSource();

    public String getExternalId() {
        return externalId;
    }

    public PostDto setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public URI getLink() {
        return link;
    }

    public PostDto setLink(URI link) {
        this.link = link;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public PostDto setPhone(String phone) {
        this.phone = phone.trim();
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PostDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public PostDto setStreet(String street) {
        this.street = street.trim();
        return this;
    }

    public String getDistrict() {
        return district;
    }

    public PostDto setDistrict(String district) {
        this.district = district.trim();
        return this;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public PostDto setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber.trim();
        return this;
    }

    public String getHeating() {
        return heating;
    }

    public PostDto setHeating(String heating) {
        this.heating = heating.trim();
        return this;
    }

    public Integer getFloor() {
        return floor;
    }

    public PostDto setFloor(Integer floor) {
        this.floor = floor;
        return this;
    }

    public Integer getTotalFloors() {
        return totalFloors;
    }

    public PostDto setTotalFloors(Integer totalFloors) {
        this.totalFloors = totalFloors;
        return this;
    }

    public Integer getArea() {
        return area;
    }

    public PostDto setArea(Integer area) {
        this.area = area;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public PostDto setPrice(BigDecimal price) {
        this.price = price.stripTrailingZeros();
        return this;
    }

    public Integer getRooms() {
        return rooms;
    }

    public PostDto setRooms(Integer rooms) {
        this.rooms = rooms;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public PostDto setYear(Integer year) {
        this.year = year;
        return this;
    }

    // TODO builder with trimming, building, formatting
}
