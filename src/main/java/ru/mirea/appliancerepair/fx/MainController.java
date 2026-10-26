package ru.mirea.appliancerepair.fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.model.Priority;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.repository.CustomerRepository;
import ru.mirea.appliancerepair.repository.RepairRequestRepository;
import ru.mirea.appliancerepair.service.CustomerService;
import ru.mirea.appliancerepair.service.RepairRequestService;
import ru.mirea.appliancerepair.util.DataExporter;
import ru.mirea.appliancerepair.util.ExcelExporter;
import ru.mirea.appliancerepair.util.Ui;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final ObservableList<RepairRequest> requests = FXCollections.observableArrayList();
    private final FilteredList<RepairRequest> filteredRequests = new FilteredList<>(requests, request -> true);
    private final SortedList<RepairRequest> sortedRequests = new SortedList<>(filteredRequests);
    private final CustomerService customerService = new CustomerService(new CustomerRepository());
    private final RepairRequestService requestService = new RepairRequestService(new RepairRequestRepository(), new CustomerRepository());
    private final DataExporter exporter = new ExcelExporter();

    @FXML private TableView<RepairRequest> repairTable;
    @FXML private TableColumn<RepairRequest, Long> idColumn;
    @FXML private TableColumn<RepairRequest, String> customerColumn;
    @FXML private TableColumn<RepairRequest, String> typeColumn;
    @FXML private TableColumn<RepairRequest, String> brandColumn;
    @FXML private TableColumn<RepairRequest, String> issueColumn;
    @FXML private TableColumn<RepairRequest, String> statusColumn;
    @FXML private TableColumn<RepairRequest, String> priorityColumn;
    @FXML private TableColumn<RepairRequest, BigDecimal> costColumn;
    @FXML private TableColumn<RepairRequest, String> dateColumn;
    @FXML private TableColumn<RepairRequest, String> urgentColumn;
    @FXML private TableColumn<RepairRequest, String> createdColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private ComboBox<String> urgentFilter;
    @FXML private Label recordsLabel;
    @FXML private Label connectionLabel;

    @FXML
    private void initialize() {
        configureTable();
        statusFilter.setItems(FXCollections.observableArrayList(
                "Все статусы", RequestStatus.NEW.toString(), RequestStatus.DIAGNOSING.toString(),
                RequestStatus.REPAIRING.toString(), RequestStatus.COMPLETED.toString(), RequestStatus.CANCELLED.toString()));
        priorityFilter.setItems(FXCollections.observableArrayList(
                "Все приоритеты", Priority.LOW.toString(), Priority.MEDIUM.toString(), Priority.HIGH.toString(), Priority.URGENT.toString()));
        urgentFilter.setItems(FXCollections.observableArrayList("Любая срочность", "Только срочные", "Не срочные"));
        statusFilter.getSelectionModel().selectFirst();
        priorityFilter.getSelectionModel().selectFirst();
        urgentFilter.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        priorityFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        urgentFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortedRequests.comparatorProperty().bind(repairTable.comparatorProperty());
        repairTable.setItems(sortedRequests);
        loadRequests();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        customerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getCustomerName()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getApplianceType()));
        brandColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getApplianceBrand()));
        issueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getIssueTitle()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus().toString()));
        priorityColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getPriority().toString()));
        costColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEstimatedCost()));
        costColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format(Locale.forLanguageTag("ru-RU"), "%,.2f", value));
            }
        });
        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getPlannedDate() == null ? "—" : cell.getValue().getPlannedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        urgentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().isUrgent() ? "Да" : "Нет"));
        createdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getCreatedAt() == null ? "—" : cell.getValue().getCreatedAt().format(DATE_TIME_FORMAT)));
    }

    private void loadRequests() {
        try {
            List<RepairRequest> fromDatabase = requestService.findAll();
            requests.setAll(fromDatabase);
            connectionLabel.setText("PostgreSQL: подключено");
            connectionLabel.setStyle("-fx-text-fill: #217452; -fx-font-weight: bold;");
            applyFilters();
        } catch (SQLException | RuntimeException e) {
            connectionLabel.setText("PostgreSQL: ошибка");
            connectionLabel.setStyle("-fx-text-fill: #b53f3f; -fx-font-weight: bold;");
            Ui.error(owner(), "Не удалось загрузить заявки", explainDatabaseError(e));
        }
    }

    private String explainDatabaseError(Exception e) {
        String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (detail.contains("Connection refused") || detail.contains("подключиться") || detail.contains("The connection attempt")) {
            return "Не удалось подключиться к PostgreSQL. Убедитесь, что Docker Desktop запущен и контейнер базы находится в состоянии healthy.\n\n" + detail;
        }
        return detail;
    }

    private void applyFilters() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedStatus = statusFilter.getValue();
        String selectedPriority = priorityFilter.getValue();
        String selectedUrgency = urgentFilter.getValue();
        filteredRequests.setPredicate(request -> {
            boolean textMatches = text.isBlank()
                    || contains(request.getCustomerName(), text)
                    || contains(request.getApplianceType(), text)
                    || contains(request.getApplianceBrand(), text)
                    || contains(request.getIssueTitle(), text)
                    || contains(request.getIssueDescription(), text);
            boolean statusMatches = selectedStatus == null || selectedStatus.equals("Все статусы")
                    || selectedStatus.equals(request.getStatus().toString());
            boolean priorityMatches = selectedPriority == null || selectedPriority.equals("Все приоритеты")
                    || selectedPriority.equals(request.getPriority().toString());
            boolean urgencyMatches = selectedUrgency == null || selectedUrgency.equals("Любая срочность")
                    || (selectedUrgency.equals("Только срочные") && request.isUrgent())
                    || (selectedUrgency.equals("Не срочные") && !request.isUrgent());
            return textMatches && statusMatches && priorityMatches && urgencyMatches;
        });
        recordsLabel.setText("Показано заявок: " + filteredRequests.size() + " из " + requests.size());
    }

    private boolean contains(String source, String search) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(search);
    }

    @FXML private void handleAdd() { openRequestDialog(null); }

    @FXML
    private void handleEdit() {
        RepairRequest selected = selectedRequest();
        if (selected != null) openRequestDialog(selected);
    }

    private void openRequestDialog(RepairRequest request) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("request-dialog.fxml"));
            Parent root = loader.load();
            RequestDialogController controller = loader.getController();
            controller.configure(requestService, customerService, request);
            Stage dialog = new Stage();
            dialog.initOwner(owner());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(request == null ? "Новая заявка" : "Изменение заявки №" + request.getId());
            dialog.setScene(new Scene(root));
            dialog.setMinWidth(620);
            dialog.setMinHeight(650);
            dialog.showAndWait();
            if (controller.isSaved()) loadRequests();
        } catch (IOException | SQLException e) {
            Ui.error(owner(), "Не удалось открыть форму заявки", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        RepairRequest selected = selectedRequest();
        if (selected == null) return;
        boolean confirmed = Ui.confirm(owner(), "Подтверждение удаления",
                "Удалить заявку №" + selected.getId() + " («" + selected.getIssueTitle() + "»)?\nДействие нельзя отменить.");
        if (!confirmed) return;
        try {
            requestService.delete(selected.getId());
            loadRequests();
            Ui.info(owner(), "Заявка удалена", "Заявка №" + selected.getId() + " успешно удалена.");
        } catch (SQLException | RuntimeException e) {
            Ui.error(owner(), "Не удалось удалить заявку", e.getMessage());
        }
    }

    @FXML
    private void handleDetails() {
        RepairRequest r = selectedRequest();
        if (r == null) return;
        String details = "Заявка №" + r.getId()
                + "\nКлиент: " + r.getCustomerName()
                + "\nТехника: " + r.getApplianceType() + " · " + r.getApplianceBrand()
                + "\nНеисправность: " + r.getIssueTitle()
                + "\nОписание: " + r.getIssueDescription()
                + "\nСтатус: " + r.getStatus()
                + "\nПриоритет: " + r.getPriority()
                + "\nСтоимость: " + r.getEstimatedCost() + " руб."
                + "\nПлановая дата: " + (r.getPlannedDate() == null ? "не задана" : r.getPlannedDate())
                + "\nСрочная: " + (r.isUrgent() ? "да" : "нет")
                + "\nСоздана: " + r.getCreatedAt();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner());
        alert.setTitle("Информация о заявке");
        alert.setHeaderText("Заявка №" + r.getId());
        alert.setContentText(details);
        alert.getDialogPane().setMinWidth(480);
        alert.showAndWait();
    }

    @FXML
    private void handleCustomers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("customer-dialog.fxml"));
            Parent root = loader.load();
            CustomerDialogController controller = loader.getController();
            controller.configure(customerService);
            Stage dialog = new Stage();
            dialog.initOwner(owner());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Клиенты сервиса");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            loadRequests();
        } catch (IOException | SQLException e) {
            Ui.error(owner(), "Не удалось открыть список клиентов", e.getMessage());
        }
    }

    @FXML
    private void handleStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("statistics-view.fxml"));
            Parent root = loader.load();
            StatisticsController controller = loader.getController();
            controller.configure(requestService, customerService);
            Stage dialog = new Stage();
            dialog.initOwner(owner());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Статистика сервиса");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException | SQLException e) {
            Ui.error(owner(), "Не удалось рассчитать статистику", e.getMessage());
        }
    }

    @FXML
    private void handleExport() {
        if (sortedRequests.isEmpty()) {
            Ui.info(owner(), "Нет данных для экспорта", "По текущим условиям поиска и фильтрам нет заявок для выгрузки.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт текущего набора заявок в Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName("repair-requests.xlsx");
        java.io.File file = chooser.showSaveDialog(owner());
        if (file == null) return;
        Path path = file.toPath();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            path = path.resolveSibling(path.getFileName() + ".xlsx");
        }
        try {
            exporter.export(new ArrayList<>(sortedRequests), path);
            Ui.info(owner(), "Экспорт завершён", "В Excel выгружено заявок: " + sortedRequests.size() + "\nФайл: " + path.toAbsolutePath());
        } catch (IOException e) {
            Ui.error(owner(), "Ошибка экспорта", "Не удалось записать Excel-файл. Проверьте права доступа и что файл не открыт в Excel.\n" + e.getMessage());
        }
    }

    @FXML private void handleRefresh() { loadRequests(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        priorityFilter.getSelectionModel().selectFirst();
        urgentFilter.getSelectionModel().selectFirst();
        applyFilters();
    }

    private RepairRequest selectedRequest() {
        RepairRequest selected = repairTable.getSelectionModel().getSelectedItem();
        if (selected == null) Ui.info(owner(), "Не выбрана заявка", "Сначала выделите строку заявки в таблице.");
        return selected;
    }

    private Stage owner() {
        return repairTable != null && repairTable.getScene() != null ? (Stage) repairTable.getScene().getWindow() : null;
    }
}
