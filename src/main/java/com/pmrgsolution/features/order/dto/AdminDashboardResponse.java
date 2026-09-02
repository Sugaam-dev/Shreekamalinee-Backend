package com.pmrgsolution.features.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    private long totalOrders;
    private long pendingOrders;
    private long pendingPaymentVerification; // orders with status = PAYMENT_PROOF_SUBMITTED
    private long deliveredOrders;
    private BigDecimal totalRevenue;
    private long totalProducts;
    private long lowStockProducts;
    private long totalCustomers;
    private List<OrderResponse> recentOrders;
    private com.pmrgsolution.features.auth.dto.EmailStatsResponse emailStats;
}