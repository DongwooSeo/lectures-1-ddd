package com.growmighty.lectures.firstday.tangledmonolith.seller.application;

import com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto.ApplySellerCommand;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto.SellerInfo;
import com.growmighty.lectures.firstday.tangledmonolith.seller.domain.Seller;
import com.growmighty.lectures.firstday.tangledmonolith.seller.domain.SellerRepository;
import com.growmighty.lectures.firstday.tangledmonolith.common.exception.EntityNotFoundException;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final SellerRepository sellerRepository;
    private final UserService userService;

    @Transactional
    public SellerInfo apply(ApplySellerCommand command) {
        userService.getUser(command.userId());
        if (sellerRepository.existsByUserId(command.userId())) {
            throw new IllegalStateException("이미 입점한 유저입니다. userId=" + command.userId());
        }
        Seller seller = Seller.apply(command.userId(), command.businessName());
        return SellerInfo.from(sellerRepository.save(seller));
    }

    @Transactional
    public void suspend(Long sellerId) {
        getSellerEntity(sellerId).suspend();
    }

    @Transactional(readOnly = true)
    public SellerInfo getSeller(Long sellerId) {
        return SellerInfo.from(getSellerEntity(sellerId));
    }

    @Transactional(readOnly = true)
    public void validateSellable(Long sellerId) {
        Seller seller = getSellerEntity(sellerId);
        if (!seller.canSell()) {
            throw new IllegalStateException("판매 가능한 셀러가 아닙니다. sellerId=" + sellerId);
        }
    }

    private Seller getSellerEntity(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 셀러입니다. sellerId=" + sellerId));
    }
}
