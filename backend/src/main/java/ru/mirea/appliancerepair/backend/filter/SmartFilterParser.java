package ru.mirea.appliancerepair.backend.filter;

import org.springframework.stereotype.Component;
import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;
import ru.mirea.appliancerepair.backend.error.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SmartFilterParser {
    private static final Pattern TOKEN = Pattern.compile("\\\"([^\\\"]*)\\\"|(\\S+)");
    private static final Pattern COST = Pattern.compile("^(?:стоимость|цена|cost)(<=|>=|=|<|>)([0-9]+(?:[.,][0-9]{1,2})?)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public RepairRequestFilter parse(String source) {
        List<String> free = new ArrayList<>();
        String type = null;
        String customer = null;
        RequestStatus status = null;
        RequestPriority priority = null;
        Boolean urgent = null;
        RepairRisk risk = null;
        String tag = null;
        Boolean archived = false;
        List<RepairRequestFilter.CostConstraint> costs = new ArrayList<>();

        if (source == null || source.isBlank()) {
            return new RepairRequestFilter("", null, null, null, null, null, null, null, false, List.of());
        }
        if (source.length() > 500) throw new BadRequestException("Строка фильтра слишком длинная");

        Matcher matcher = TOKEN.matcher(source.trim());
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String token = raw.trim();
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);

            Matcher costMatcher = COST.matcher(lower);
            if (costMatcher.matches()) {
                BigDecimal value = new BigDecimal(costMatcher.group(2).replace(',', '.'));
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("1000000")) > 0)
                    throw new BadRequestException("Стоимость должна быть от 0 до 1000000");
                costs.add(new RepairRequestFilter.CostConstraint(operator(costMatcher.group(1)), value));
                continue;
            }

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = lower.substring(0, colon);
                String value = token.substring(colon + 1).trim();
                String normalized = value.toLowerCase(Locale.ROOT);
                switch (key) {
                    case "тип", "техника", "type", "appliance" -> type = value;
                    case "клиент", "заказчик", "customer" -> customer = value;
                    case "статус", "status" -> status = status(normalized);
                    case "приоритет", "priority" -> priority = priority(normalized);
                    case "срочно", "urgent" -> urgent = booleanValue(normalized, "срочно");
                    case "риск", "risk" -> risk = risk(normalized);
                    case "тег", "tag" -> tag = value;
                    case "архив", "archived" -> archived = switch (normalized) {
                        case "все", "all", "любой", "any" -> null;
                        default -> booleanValue(normalized, "архив");
                    };
                    default -> free.add(token);
                }
            } else {
                free.add(token);
            }
        }
        return new RepairRequestFilter(String.join(" ", free), blankToNull(type), blankToNull(customer),
                status, priority, urgent, risk, blankToNull(tag), archived, List.copyOf(costs));
    }

    private static RepairRequestFilter.Operator operator(String value) {
        return switch (value) {
            case "<" -> RepairRequestFilter.Operator.LT;
            case "<=" -> RepairRequestFilter.Operator.LTE;
            case "=" -> RepairRequestFilter.Operator.EQ;
            case ">=" -> RepairRequestFilter.Operator.GTE;
            case ">" -> RepairRequestFilter.Operator.GT;
            default -> throw new BadRequestException("Неизвестный оператор стоимости");
        };
    }

    private static RequestStatus status(String value) {
        return switch (value.replace('-', '_')) {
            case "новая", "new" -> RequestStatus.NEW;
            case "диагностика", "diagnosing" -> RequestStatus.DIAGNOSING;
            case "ремонт", "в_ремонте", "repairing" -> RequestStatus.REPAIRING;
            case "готова", "завершена", "completed" -> RequestStatus.COMPLETED;
            case "отменена", "отмена", "cancelled", "canceled" -> RequestStatus.CANCELLED;
            default -> throw new BadRequestException("Неизвестный статус заявки");
        };
    }

    private static RequestPriority priority(String value) {
        return switch (value) {
            case "низкий", "low" -> RequestPriority.LOW;
            case "средний", "medium" -> RequestPriority.MEDIUM;
            case "высокий", "high" -> RequestPriority.HIGH;
            case "срочный", "urgent" -> RequestPriority.URGENT;
            default -> throw new BadRequestException("Неизвестный приоритет заявки");
        };
    }

    private static RepairRisk risk(String value) {
        return switch (value) {
            case "не_применимо", "not_applicable" -> RepairRisk.NOT_APPLICABLE;
            case "низкий", "low" -> RepairRisk.LOW;
            case "средний", "medium" -> RepairRisk.MEDIUM;
            case "высокий", "high" -> RepairRisk.HIGH;
            case "критический", "critical" -> RepairRisk.CRITICAL;
            default -> throw new BadRequestException("Неизвестный уровень риска ремонта");
        };
    }

    private static Boolean booleanValue(String value, String label) {
        return switch (value) {
            case "true", "да", "1", "yes" -> true;
            case "false", "нет", "0", "no" -> false;
            default -> throw new BadRequestException("Некорректное логическое значение: " + label);
        };
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
