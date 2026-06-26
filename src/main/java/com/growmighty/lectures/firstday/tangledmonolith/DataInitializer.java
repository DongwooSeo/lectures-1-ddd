package com.growmighty.lectures.firstday.tangledmonolith;

import com.growmighty.lectures.firstday.tangledmonolith.cart.Cart;
import com.growmighty.lectures.firstday.tangledmonolith.cart.CartItem;
import com.growmighty.lectures.firstday.tangledmonolith.cart.CartRepository;
import com.growmighty.lectures.firstday.tangledmonolith.product.Product;
import com.growmighty.lectures.firstday.tangledmonolith.product.ProductRepository;
import com.growmighty.lectures.firstday.tangledmonolith.seller.Seller;
import com.growmighty.lectures.firstday.tangledmonolith.seller.SellerRepository;
import com.growmighty.lectures.firstday.tangledmonolith.user.User;
import com.growmighty.lectures.firstday.tangledmonolith.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @Override
    public void run(String... args) {
        User buyer = userRepository.save(
                User.register("buyer@growmighty.co.kr", "encoded-pw", "구매자", "010-1111-1111"));
        User sellerOwner = userRepository.save(
                User.register("seller@growmighty.co.kr", "encoded-pw", "판매자", "010-2222-2222"));

        Seller seller = sellerRepository.save(Seller.create(sellerOwner.getId()));

        Product chair = productRepository.save(
                Product.create(seller.getId(), "Dofia 이동식 접이식 식탁 의자 4개 세트", BigDecimal.valueOf(179000), 10, "가정용 소형주택 신축식"));
        Product table = productRepository.save(
                Product.create(seller.getId(), "원목 4인용 식탁", BigDecimal.valueOf(259000), 5, "북유럽 스타일 원목 식탁"));

        Cart cart = Cart.create(buyer.getId());
        cart.addItem(CartItem.create(chair.getId(), 2));
        cart.addItem(CartItem.create(table.getId(), 1));
        cartRepository.save(cart);

        System.out.printf(
                "[seed] 구매자 id=%d, 장바구니 id=%d 준비 완료. 예) POST /orders?userId=%d%n",
                buyer.getId(), cart.getId(), buyer.getId());
    }
}
