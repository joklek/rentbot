package com.joklek.rentbot.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "post_price_history")
public class PostPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private BigDecimal price;
    @NotNull
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    public PostPriceHistory() {
    }

    public PostPriceHistory(BigDecimal price) {
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PostPriceHistory setId(Long id) {
        this.id = id;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public PostPriceHistory setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PostPriceHistory setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Post getPost() {
        return post;
    }

    public PostPriceHistory setPost(Post post) {
        this.post = post;
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
        var priceHistory = (PostPriceHistory) o;
        return this.id != null &&
                Objects.equals(this.id, priceHistory.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
