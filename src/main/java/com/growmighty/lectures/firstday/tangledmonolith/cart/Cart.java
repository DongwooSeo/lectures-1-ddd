package com.growmighty.lectures.firstday.tangledmonolith.cart;

import com.growmighty.lectures.firstday.tangledmonolith.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public static Cart create(User user) {
        Cart cart = new Cart();
        cart.user = user;
        return cart;
    }

    public void addItem(CartItem item) {
        this.items.add(item);

        if (item.getCart() != this) {
            item.assignCart(this);
        }
    }

    public void clear() {
        this.items.clear();
    }
}
