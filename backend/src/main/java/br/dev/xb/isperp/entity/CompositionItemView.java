package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("java:S2583")
public interface CompositionItemView {

    @Nullable
    UUID getId();

    void setId(UUID id);

    @Nullable
    LocalDateTime getCreatedAt();

    void setCreatedAt(LocalDateTime createdAt);

    @Nullable
    PlanCompositionCategory getCategory();

    void setCategory(PlanCompositionCategory category);

    @Nullable
    BigDecimal getAmount();

    default void initDefaults() {
        if (getId() == null) {
            setId(UuidCreatorUtils.generateUuidV7());
        }
        if (getCreatedAt() == null) {
            setCreatedAt(LocalDateTime.now());
        }
        if (getCategory() == null) {
            setCategory(PlanCompositionCategory.SCM);
        }
    }

    static BigDecimal[] calculateScmAndSvaTotals(List<? extends CompositionItemView> items) {
        BigDecimal scm = BigDecimal.ZERO;
        BigDecimal sva = BigDecimal.ZERO;
        for (CompositionItemView item : items) {
            item.initDefaults();
            BigDecimal itemAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            if (item.getCategory() == PlanCompositionCategory.SCM) {
                scm = scm.add(itemAmt);
            } else {
                sva = sva.add(itemAmt);
            }
        }
        return new BigDecimal[]{scm, sva};
    }
}
