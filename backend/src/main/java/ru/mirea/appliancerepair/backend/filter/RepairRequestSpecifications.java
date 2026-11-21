package ru.mirea.appliancerepair.backend.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.appliancerepair.backend.domain.ApplianceType;
import ru.mirea.appliancerepair.backend.domain.Customer;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RepairRequestSpecifications {
    private RepairRequestSpecifications() { }

    public static Specification<RepairRequest> from(RepairRequestFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.priority() != null) predicates.add(cb.equal(root.get("priority"), filter.priority()));
            if (filter.urgent() != null) predicates.add(cb.equal(root.get("urgent"), filter.urgent()));
            if (filter.risk() != null) predicates.add(cb.equal(root.get("riskLevel"), filter.risk()));

            Join<RepairRequest, Customer> customer = root.join("customer", JoinType.LEFT);
            Join<RepairRequest, ApplianceType> type = root.join("applianceType", JoinType.LEFT);

            if (filter.applianceType() != null) {
                String like = "%" + escape(filter.applianceType()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("applianceTypeName")), like, '\\'),
                        cb.like(cb.lower(type.get("name")), like, '\\')));
            }
            if (filter.customer() != null) {
                predicates.add(cb.like(cb.lower(customer.get("fullName")),
                        "%" + escape(filter.customer()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<RepairRequest, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requestNumber")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(root.get("applianceTypeName")), like, '\\'),
                        cb.like(cb.lower(root.get("applianceBrand")), like, '\\'),
                        cb.like(cb.lower(root.get("issueTitle")), like, '\\'),
                        cb.like(cb.lower(root.get("issueDescription")), like, '\\'),
                        cb.like(cb.lower(customer.get("fullName")), like, '\\'),
                        cb.like(cb.lower(customer.get("phone")), like, '\\')));
            }
            filter.costs().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.<java.math.BigDecimal>get("estimatedCost"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.<java.math.BigDecimal>get("estimatedCost"), constraint.value());
                case EQ -> cb.equal(root.get("estimatedCost"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.<java.math.BigDecimal>get("estimatedCost"), constraint.value());
                case GT -> cb.greaterThan(root.<java.math.BigDecimal>get("estimatedCost"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
