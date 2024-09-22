package com.joklek.rentbot.scraper;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

public abstract class PostDto {
    private String externalId;
    private URI link;
    private String description;
    private String street;
    private String district;
    private String houseNumber;
    private String heating;
    private Integer floor;
    private Integer totalFloors;
    private BigDecimal area;
    private BigDecimal price;
    private Integer rooms;
    private Integer year;
    private String buildingState;
    private String buildingMaterial;

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

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public PostDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public Optional<String> getStreet() {
        return Optional.ofNullable(street);
    }

    public PostDto setStreet(String street) {
        this.street = street;
        return this;
    }

    public Optional<String> getDistrict() {
        return Optional.ofNullable(district);
    }

    public PostDto setDistrict(String district) {
        this.district = district;
        return this;
    }

    public Optional<String> getHouseNumber() {
        return Optional.ofNullable(houseNumber);
    }

    public PostDto setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
        return this;
    }

    public Optional<String> getHeating() {
        return Optional.ofNullable(heating);
    }

    public PostDto setHeating(String heating) {
        this.heating = heating;
        return this;
    }

    public Optional<Integer> getFloor() {
        return Optional.ofNullable(floor);
    }

    public PostDto setFloor(Integer floor) {
        this.floor = floor;
        return this;
    }

    public Optional<Integer> getTotalFloors() {
        return Optional.ofNullable(totalFloors);
    }

    public PostDto setTotalFloors(Integer totalFloors) {
        this.totalFloors = totalFloors;
        return this;
    }

    public Optional<BigDecimal> getArea() {
        return Optional.ofNullable(area);
    }

    public PostDto setArea(BigDecimal area) {
        this.area = area;
        return this;
    }

    public Optional<BigDecimal> getPrice() {
        return Optional.ofNullable(price);
    }

    public PostDto setPrice(BigDecimal price) {
        this.price = price.stripTrailingZeros();
        return this;
    }

    public Optional<Integer> getRooms() {
        return Optional.ofNullable(rooms);
    }

    public PostDto setRooms(Integer rooms) {
        this.rooms = rooms;
        return this;
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public PostDto setYear(Integer year) {
        this.year = year;
        return this;
    }

    public Optional<String> getBuildingMaterial() {
        return Optional.ofNullable(buildingMaterial);
    }

    public PostDto setBuildingMaterial(String buildingMaterial) {
        this.buildingMaterial = buildingMaterial;
        return this;
    }

    public Optional<String> getBuildingState() {
        return Optional.ofNullable(buildingState);
    }

    public PostDto setBuildingState(String buildingState) {
        this.buildingState = buildingState;
        return this;
    }
}
