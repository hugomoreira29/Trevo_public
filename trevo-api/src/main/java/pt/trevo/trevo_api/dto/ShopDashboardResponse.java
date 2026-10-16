package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShopDashboardResponse(
    ShopResponse shop,
    List<DashboardOrder> orders,
    Revenue revenue,
    int productCount
) {

    public record DashboardOrder(Long id, String status, String buyerName) {}

    public record Revenue(BigDecimal total, String currency) {}
}
