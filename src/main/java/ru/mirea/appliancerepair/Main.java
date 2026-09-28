package ru.mirea.appliancerepair;

import ru.mirea.appliancerepair.exception.BusinessException;
import ru.mirea.appliancerepair.exception.EntityNotFoundException;
import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.model.Priority;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.repository.CustomerRepository;
import ru.mirea.appliancerepair.repository.RepairRequestRepository;
import ru.mirea.appliancerepair.service.CustomerService;
import ru.mirea.appliancerepair.service.RepairRequestService;
import ru.mirea.appliancerepair.util.DataExporter;
import ru.mirea.appliancerepair.util.DatabaseManager;
import ru.mirea.appliancerepair.util.ExcelExporter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final CustomerService customerService;
    private final RepairRequestService repairRequestService;
    private final DataExporter exporter = new ExcelExporter();

    public Main() {
        CustomerRepository customerRepository = new CustomerRepository();
        this.customerService = new CustomerService(customerRepository);
        this.repairRequestService = new RepairRequestService(new RepairRequestRepository(), customerRepository);
    }

    public static void main(String[] args) {
        Main application = new Main();
        application.run();
    }

    private void run() {
        System.out.println("============================================");
        System.out.println("СЕРВИС РЕМОНТА БЫТОВОЙ ТЕХНИКИ");
        System.out.println("============================================");
        try (Connection ignored = DatabaseManager.getConnection()) {
            System.out.println("Подключение к PostgreSQL: успешно");
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к базе данных: " + e.getMessage());
            return;
        }

        while (true) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> customerMenu();
                    case "2" -> requestMenu();
                    case "3" -> searchMenu();
                    case "4" -> filterMenu();
                    case "5" -> statistics();
                    case "6" -> exportData();
                    case "7" -> printDatabaseTables();
                    case "0" -> {
                        System.out.println("Программа завершена.");
                        return;
                    }
                    default -> System.out.println("Ошибка: выберите пункт от 0 до 7.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Неизвестная ошибка: " + e.getMessage());
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("--------------- ГЛАВНОЕ МЕНЮ ---------------");
        System.out.println("1. Клиенты");
        System.out.println("2. Заявки на ремонт");
        System.out.println("3. Поиск");
        System.out.println("4. Фильтрация");
        System.out.println("5. Статистика");
        System.out.println("6. Экспорт данных в Excel");
        System.out.println("7. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private void customerMenu() throws SQLException {
        while (true) {
            System.out.println();
            System.out.println("--------------- КЛИЕНТЫ ---------------");
            System.out.println("1. Создать клиента");
            System.out.println("2. Показать всех клиентов");
            System.out.println("3. Найти клиента по ID");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createCustomer();
                case "2" -> printCustomers(customerService.findAll());
                case "3" -> showCustomer();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Ошибка: неверный пункт меню.");
            }
        }
    }

    private void createCustomer() throws SQLException {
        System.out.print("ФИО: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Телефон: ");
        String phone = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Адрес: ");
        String address = scanner.nextLine().trim();
        Customer customer = customerService.create(new Customer(fullName, phone, email, address));
        System.out.println("Клиент создан. ID: " + customer.getId());
    }

    private void showCustomer() throws SQLException {
        long id = readLong("ID клиента: ");
        printCustomers(List.of(customerService.requireById(id)));
    }

    private void requestMenu() throws SQLException {
        while (true) {
            System.out.println();
            System.out.println("------------- ЗАЯВКИ НА РЕМОНТ -------------");
            System.out.println("1. Создать заявку");
            System.out.println("2. Показать все заявки");
            System.out.println("3. Получить заявку по ID");
            System.out.println("4. Изменить заявку");
            System.out.println("5. Удалить заявку");
            System.out.println("6. Сортировка по дате");
            System.out.println("7. Сортировка по стоимости");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createRequest();
                case "2" -> printRequests(repairRequestService.findAll());
                case "3" -> showRequest();
                case "4" -> updateRequest();
                case "5" -> deleteRequest();
                case "6" -> printRequests(repairRequestService.sortByDate(repairRequestService.findAll()));
                case "7" -> printRequests(repairRequestService.sortByCostDescending(repairRequestService.findAll()));
                case "0" -> {
                    return;
                }
                default -> System.out.println("Ошибка: неверный пункт меню.");
            }
        }
    }

    private void createRequest() throws SQLException {
        printCustomers(customerService.findAll());
        long customerId = readLong("ID клиента: ");
        System.out.print("Тип техники: ");
        String applianceType = scanner.nextLine().trim();
        System.out.print("Бренд: ");
        String brand = scanner.nextLine().trim();
        System.out.print("Неисправность: ");
        String issueTitle = scanner.nextLine().trim();
        System.out.print("Описание: ");
        String description = scanner.nextLine().trim();
        Priority priority = readPriority();
        BigDecimal cost = readDecimal("Ориентировочная стоимость: ");
        LocalDate plannedDate = readOptionalDate("Плановая дата YYYY-MM-DD (Enter — без даты): ");
        boolean urgent = readBoolean("Срочная заявка (да/нет): ");
        RepairRequest request = new RepairRequest(null, customerId, null, applianceType, brand, issueTitle, description,
                RequestStatus.NEW, priority, cost, plannedDate, urgent, null);
        RepairRequest saved = repairRequestService.create(request);
        System.out.println("Заявка создана. ID: " + saved.getId());
    }

    private void showRequest() throws SQLException {
        long id = readLong("ID заявки: ");
        printRequests(List.of(repairRequestService.requireById(id)));
    }

    private void updateRequest() throws SQLException {
        long id = readLong("ID заявки: ");
        RepairRequest current = repairRequestService.requireById(id);
        System.out.println("Текущий статус: " + current.getStatus());
        printCustomers(customerService.findAll());
        long customerId = readLongWithDefault("ID клиента", current.getCustomerId());
        System.out.print("Тип техники [" + current.getApplianceType() + "]: ");
        String applianceType = scanner.nextLine().trim();
        if (applianceType.isBlank()) applianceType = current.getApplianceType();
        System.out.print("Бренд [" + current.getApplianceBrand() + "]: ");
        String brand = scanner.nextLine().trim();
        if (brand.isBlank()) brand = current.getApplianceBrand();
        System.out.print("Неисправность [" + current.getIssueTitle() + "]: ");
        String issueTitle = scanner.nextLine().trim();
        if (issueTitle.isBlank()) issueTitle = current.getIssueTitle();
        System.out.print("Описание [" + current.getIssueDescription() + "]: ");
        String description = scanner.nextLine().trim();
        if (description.isBlank()) description = current.getIssueDescription();
        RequestStatus status = readStatusWithDefault(current.getStatus());
        Priority priority = readPriorityWithDefault(current.getPriority());
        BigDecimal cost = readDecimalWithDefault("Ориентировочная стоимость", current.getEstimatedCost());
        LocalDate plannedDate = readOptionalDateWithDefault("Плановая дата", current.getPlannedDate());
        boolean urgent = readBooleanWithDefault("Срочная заявка", current.isUrgent());
        RepairRequest request = new RepairRequest(id, customerId, current.getCustomerName(), applianceType, brand, issueTitle,
                description, status, priority, cost, plannedDate, urgent, current.getCreatedAt());
        repairRequestService.update(request);
        System.out.println("Заявка изменена.");
    }

    private void deleteRequest() throws SQLException {
        long id = readLong("ID заявки: ");
        RepairRequest request = repairRequestService.requireById(id);
        System.out.println("Удалить заявку: " + request.getIssueTitle() + "? Это действие необратимо.");
        if (readBoolean("Подтвердить (да/нет): ")) {
            repairRequestService.delete(id);
            System.out.println("Заявка удалена.");
        } else {
            System.out.println("Удаление отменено.");
        }
    }

    private void searchMenu() throws SQLException {
        System.out.println();
        System.out.println("---------------- ПОИСК ----------------");
        System.out.println("1. По названию неисправности");
        System.out.println("2. По бренду техники");
        System.out.println("3. По имени клиента");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");
        String choice = scanner.nextLine().trim();
        List<RepairRequest> result = switch (choice) {
            case "1" -> repairRequestService.searchByTitle(readText("Текст поиска: "));
            case "2" -> repairRequestService.searchByBrand(readText("Бренд: "));
            case "3" -> repairRequestService.searchByCustomer(readText("Клиент: "));
            case "0" -> new ArrayList<>();
            default -> {
                System.out.println("Ошибка: неверный пункт меню.");
                yield new ArrayList<>();
            }
        };
        if (!choice.equals("0")) printRequests(result);
    }

    private void filterMenu() throws SQLException {
        System.out.println();
        System.out.println("-------------- ФИЛЬТРАЦИЯ --------------");
        System.out.println("1. По статусу");
        System.out.println("2. По приоритету");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> printRequests(repairRequestService.filterByStatus(readStatus()));
            case "2" -> printRequests(repairRequestService.filterByPriority(readPriority()));
            case "0" -> {
            }
            default -> System.out.println("Ошибка: неверный пункт меню.");
        }
    }

    private void statistics() throws SQLException {
        List<Customer> customers = customerService.findAll();
        List<RepairRequest> requests = repairRequestService.findAll();
        long completed = requests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
        long newRequests = requests.stream().filter(r -> r.getStatus() == RequestStatus.NEW).count();
        long inProgress = requests.stream().filter(r -> r.getStatus() == RequestStatus.DIAGNOSING || r.getStatus() == RequestStatus.REPAIRING).count();
        long urgent = requests.stream().filter(RepairRequest::isUrgent).count();
        double averageCost = requests.stream().map(RepairRequest::getEstimatedCost).mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        System.out.println();
        System.out.println("--------------- СТАТИСТИКА ---------------");
        System.out.println("Всего клиентов: " + customers.size());
        System.out.println("Всего заявок: " + requests.size());
        System.out.println("Новых заявок: " + newRequests);
        System.out.println("Заявок в работе: " + inProgress);
        System.out.println("Завершенных заявок: " + completed);
        System.out.println("Срочных заявок: " + urgent);
        System.out.printf("Средняя ориентировочная стоимость: %.2f%n", averageCost);
    }

    private void exportData() throws Exception {
        List<RepairRequest> requests = repairRequestService.findAll();
        System.out.print("Путь к файлу .xlsx: ");
        Path path = Path.of(scanner.nextLine().trim()).toAbsolutePath();
        exporter.export(requests, path);
        System.out.println("Экспорт завершен: " + path);
    }

    private void printDatabaseTables() throws SQLException {
        System.out.println();
        System.out.println("================ CUSTOMERS ================");
        printCustomers(customerService.findAll());
        System.out.println();
        System.out.println("============= REPAIR_REQUESTS =============");
        printRequests(repairRequestService.findAll());
    }

    private void printCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("Нет данных.");
            return;
        }
        System.out.printf("%-4s %-26s %-16s %-28s %-28s%n", "ID", "ФИО", "Телефон", "Email", "Адрес");
        for (Customer customer : customers) {
            System.out.printf("%-4d %-26s %-16s %-28s %-28s%n", customer.getId(), customer.getFullName(), customer.getPhone(), customer.getEmail(), customer.getAddress());
        }
    }

    private void printRequests(List<RepairRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Нет данных.");
            return;
        }
        System.out.printf("%-4s %-20s %-15s %-14s %-26s %-12s %-10s %-12s %-12s %-8s%n", "ID", "Клиент", "Техника", "Бренд", "Неисправность", "Статус", "Приоритет", "Стоимость", "Плановая дата", "Сроч.");
        for (RepairRequest r : requests) {
            System.out.printf("%-4d %-20s %-15s %-14s %-26s %-12s %-10s %-12s %-12s %-8s%n",
                    r.getId(), truncate(r.getCustomerName(), 20), truncate(r.getApplianceType(), 15), truncate(r.getApplianceBrand(), 14), truncate(r.getIssueTitle(), 26), r.getStatus(), r.getPriority(), r.getEstimatedCost(), r.getPlannedDate() == null ? "-" : r.getPlannedDate(), r.isUrgent() ? "ДА" : "НЕТ");
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    private long readLongWithDefault(String label, long defaultValue) {
        System.out.print(label + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isBlank()) return defaultValue;
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            throw new BusinessException("Значение должно быть целым числом");
        }
    }

    private String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private BigDecimal readDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(',', '.');
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите число, например 2500.00");
            }
        }
    }

    private BigDecimal readDecimalWithDefault(String label, BigDecimal defaultValue) {
        System.out.print(label + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim().replace(',', '.');
        if (input.isBlank()) return defaultValue;
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException e) {
            throw new BusinessException("Некорректное числовое значение");
        }
    }

    private LocalDate readOptionalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isBlank()) return null;
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата должна быть в формате YYYY-MM-DD.");
            }
        }
    }

    private LocalDate readOptionalDateWithDefault(String label, LocalDate defaultValue) {
        System.out.print(label + " [" + (defaultValue == null ? "-" : defaultValue) + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isBlank()) return defaultValue;
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Дата должна быть в формате YYYY-MM-DD");
        }
    }

    private boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("да") || input.equals("д") || input.equals("yes") || input.equals("y")) return true;
            if (input.equals("нет") || input.equals("н") || input.equals("no") || input.equals("n")) return false;
            System.out.println("Ошибка: введите да или нет.");
        }
    }

    private boolean readBooleanWithDefault(String label, boolean defaultValue) {
        System.out.print(label + " [" + (defaultValue ? "да" : "нет") + "]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.isBlank()) return defaultValue;
        if (input.equals("да") || input.equals("д") || input.equals("yes") || input.equals("y")) return true;
        if (input.equals("нет") || input.equals("н") || input.equals("no") || input.equals("n")) return false;
        throw new BusinessException("Введите да или нет");
    }

    private RequestStatus readStatus() {
        System.out.println("1. NEW");
        System.out.println("2. DIAGNOSING");
        System.out.println("3. REPAIRING");
        System.out.println("4. COMPLETED");
        System.out.println("5. CANCELLED");
        int choice = (int) readLong("Статус: ");
        return switch (choice) {
            case 1 -> RequestStatus.NEW;
            case 2 -> RequestStatus.DIAGNOSING;
            case 3 -> RequestStatus.REPAIRING;
            case 4 -> RequestStatus.COMPLETED;
            case 5 -> RequestStatus.CANCELLED;
            default -> throw new BusinessException("Некорректный статус");
        };
    }

    private RequestStatus readStatusWithDefault(RequestStatus defaultValue) {
        System.out.print("Статус [" + defaultValue + "] (Enter — без изменения): ");
        String input = scanner.nextLine().trim();
        if (input.isBlank()) return defaultValue;
        try {
            return RequestStatus.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Некорректный статус");
        }
    }

    private Priority readPriority() {
        System.out.println("1. LOW");
        System.out.println("2. MEDIUM");
        System.out.println("3. HIGH");
        System.out.println("4. URGENT");
        int choice = (int) readLong("Приоритет: ");
        return switch (choice) {
            case 1 -> Priority.LOW;
            case 2 -> Priority.MEDIUM;
            case 3 -> Priority.HIGH;
            case 4 -> Priority.URGENT;
            default -> throw new BusinessException("Некорректный приоритет");
        };
    }

    private Priority readPriorityWithDefault(Priority defaultValue) {
        System.out.print("Приоритет [" + defaultValue + "] (Enter — без изменения): ");
        String input = scanner.nextLine().trim();
        if (input.isBlank()) return defaultValue;
        try {
            return Priority.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Некорректный приоритет");
        }
    }
}
