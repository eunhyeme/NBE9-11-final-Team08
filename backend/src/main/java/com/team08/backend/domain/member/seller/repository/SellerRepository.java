package com.team08.backend.domain.member.seller.repository;

import com.team08.backend.domain.member.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
}
