package com.awin.merchantdashboard.todo.repository;

import com.awin.merchantdashboard.todo.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    long countByMerchantIdAndMappedFalse(long merchantId);
}
