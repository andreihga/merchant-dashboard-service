package com.awin.merchantdashboard.todo.service;

import com.awin.merchantdashboard.api.model.TodoList;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.todo.model.ApplicationStatus;
import com.awin.merchantdashboard.todo.model.OfferStatus;
import com.awin.merchantdashboard.todo.repository.AffiliateApplicationRepository;
import com.awin.merchantdashboard.todo.repository.OfferRepository;
import com.awin.merchantdashboard.todo.repository.ProductCategoryRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

    private final OfferRepository offers;
    private final ProductCategoryRepository categories;
    private final SaleTransactionRepository sales;
    private final AffiliateApplicationRepository applications;
    private final Clock clock;

    public TodoService(
            OfferRepository offers,
            ProductCategoryRepository categories,
            SaleTransactionRepository sales,
            AffiliateApplicationRepository applications,
            Clock clock) {
        this.offers = offers;
        this.categories = categories;
        this.sales = sales;
        this.applications = applications;
        this.clock = clock;
    }

    /** The caller must already have passed the merchant access check. */
    @Transactional(readOnly = true)
    public TodoList getTodos(long merchantId) {
        long expiredOffers =
                offers.countByMerchantIdAndStatusAndExpiresAtBefore(merchantId, OfferStatus.ACTIVE, clock.instant());
        long categoriesToMap = categories.countByMerchantIdAndMappedFalse(merchantId);
        long commissionsToValidate = sales.countByMerchantIdAndStatus(merchantId, SaleStatus.PENDING);
        long applicationsToReview = applications.countByMerchantIdAndStatus(merchantId, ApplicationStatus.PENDING);
        return new TodoList(expiredOffers, categoriesToMap, commissionsToValidate, applicationsToReview);
    }
}
