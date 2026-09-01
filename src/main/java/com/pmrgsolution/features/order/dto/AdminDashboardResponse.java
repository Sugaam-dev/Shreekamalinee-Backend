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
    private long deliveredOrders;
    private BigDecimal totalRevenue;
    private long totalProducts;
    private long lowStockProducts;
    private long totalCustomers;
    private List<OrderResponse> recentOrders;
}