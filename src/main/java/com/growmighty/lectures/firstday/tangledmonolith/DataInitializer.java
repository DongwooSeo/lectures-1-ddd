package com.growmighty.lectures.firstday.tangledmonolith;

import com.growmighty.lectures.firstday.tangledmonolith.cart.application.CartService;
import com.growmighty.lectures.firstday.tangledmonolith.cart.application.dto.AddCartItemCommand;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.ProductService;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.ProductInfo;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.RegisterProductCommand;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.SellerService;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto.ApplySellerCommand;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto.SellerInfo;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.UserService;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.dto.RegisterUserCommand;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserService userService;
    private final SellerService sellerService;
    private final ProductService productService;
    private final CartService cartService;

    @Override
    public void run(String... args) {
        UserInfo buyer = userService.register(
                new RegisterUserCommand("buyer@growmighty.co.kr", "rawPassword1!", "구매자", "010-1111-1111"));
        UserInfo sellerOwner = userService.register(
                new RegisterUserCommand("seller@growmighty.co.kr", "rawPassword2!", "판매자", "010-2222-2222"));

        SellerInfo seller = sellerService.apply(new ApplySellerCommand(sellerOwner.id(), "그로마이티 가구"));

        ProductInfo chair = productService.register(new RegisterProductCommand(
                seller.id(), "Dofia 이동식 접이식 식탁 의자 4개 세트", BigDecimal.valueOf(179000), 10, "가정용 소형주택 신축식"));
        ProductInfo table = productService.register(new RegisterProductCommand(
                seller.id(), "원목 4인용 식탁", BigDecimal.valueOf(259000), 5, "북유럽 스타일 원목 식탁"));

        cartService.addItem(new AddCartItemCommand(buyer.id(), chair.id(), 2));
        cartService.addItem(new AddCartItemCommand(buyer.id(), table.id(), 1));

        System.out.printf(
                "[seed] 구매자 id=%d 준비 완료. 예) POST /orders/from-cart?userId=%d 또는 POST /orders%n",
                buyer.id(), buyer.id());
    }
}
