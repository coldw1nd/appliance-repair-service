package ru.mirea.appliancerepair.fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.appliancerepair.exception.BusinessException;
import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.model.Priority;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.service.CustomerService;
import ru.mirea.appliancerepair.service.RepairRequestService;
import ru.mirea.appliancerepair.util.Ui;

import java.math.BigDecimal;
import java.sql.SQLException;

public class RequestDialogController {
    @FXML private Label dialogTitle;
    @FXML private ComboBox<Customer> customerBox;
    @FXML private TextField typeField;
    @FXML private TextField brandField;
    @FXML private TextField issueField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<RequestStatus> statusBox;
    @FXML private ComboBox<Priority> priorityBox;
    @FXML private TextField costField;
    @FXML private DatePicker plannedDatePicker;
    @FXML private CheckBox urgentCheckBox;
    @FXML private Button saveButton;

    private RepairRequestService requestService;
    private CustomerService customerService;
    private RepairRequest existing;
    private boolean saved;

    public void configure(RepairRequestService requestService, CustomerService customerService, RepairRequest existing) throws SQLException {
        this.requestService = requestService;
        this.customerService = customerService;
        this.existing = existing;
        customerBox.setItems(FXCollections.observableArrayList(customerService.findAll()));
        statusBox.setItems(FXCollections.observableArrayList(RequestStatus.values()));
        priorityBox.setItems(FXCollections.observableArrayList(Priority.values()));
        if (customerBox.getItems().isEmpty()) {
            saveButton.setDisable(true);
            Ui.error(null, "Нет клиентов", "Сначала добавьте клиента через кнопку «Клиенты», затем создайте заявку.");
        }
        if (existing == null) {
            dialogTitle.setText("Новая заявка на ремонт");
            statusBox.setValue(RequestStatus.NEW);
            statusBox.setDisable(true);
            priorityBox.setValue(Priority.MEDIUM);
            urgentCheckBox.setSelected(false);
            if (!customerBox.getItems().isEmpty()) customerBox.getSelectionModel().selectFirst();
        } else {
            dialogTitle.setText("Изменение заявки №" + existing.getId());
            customerBox.getItems().stream().filter(c -> c.getId().equals(existing.getCustomerId())).findFirst()
                    .ifPresent(customerBox::setValue);
            typeField.setText(existing.getApplianceType());
            brandField.setText(existing.getApplianceBrand());
            issueField.setText(existing.getIssueTitle());
            descriptionField.setText(existing.getIssueDescription());
            statusBox.setValue(existing.getStatus());
            priorityBox.setValue(existing.getPriority());
            costField.setText(existing.getEstimatedCost().toPlainString());
            plannedDatePicker.setValue(existing.getPlannedDate());
            urgentCheckBox.setSelected(existing.isUrgent());
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void handleSave() {
        try {
            Customer customer = customerBox.getValue();
            if (customer == null) throw new BusinessException("Выберите клиента из списка.");
            String applianceType = typeField.getText() == null ? "" : typeField.getText().trim();
            String brand = brandField.getText() == null ? "" : brandField.getText().trim();
            String issue = issueField.getText() == null ? "" : issueField.getText().trim();
            String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
            String costText = costField.getText() == null ? "" : costField.getText().trim().replace(',', '.');
            if (applianceType.isBlank()) throw new BusinessException("Заполните поле «Тип техники».");
            if (brand.isBlank()) throw new BusinessException("Заполните поле «Бренд».");
            if (issue.isBlank()) throw new BusinessException("Заполните поле «Неисправность».");
            if (description.isBlank()) throw new BusinessException("Заполните поле «Описание».");
            if (costText.isBlank()) throw new BusinessException("Заполните поле «Стоимость».");
            BigDecimal cost;
            try {
                cost = new BigDecimal(costText);
            } catch (NumberFormatException e) {
                throw new BusinessException("Стоимость должна быть числом, например 4500.00.");
            }
            RequestStatus status = existing == null ? RequestStatus.NEW : statusBox.getValue();
            RepairRequest request = new RepairRequest(
                    existing == null ? null : existing.getId(),
                    customer.getId(), customer.getFullName(), applianceType, brand, issue, description,
                    status, priorityBox.getValue(), cost, plannedDatePicker.getValue(), urgentCheckBox.isSelected(),
                    existing == null ? null : existing.getCreatedAt());
            if (existing == null) requestService.create(request);
            else requestService.update(request);
            saved = true;
            closeWindow();
        } catch (BusinessException e) {
            Ui.error(window(), "Проверьте данные", e.getMessage());
        } catch (SQLException e) {
            Ui.error(window(), "Ошибка базы данных", databaseMessage(e));
        } catch (RuntimeException e) {
            Ui.error(window(), "Не удалось сохранить заявку", e.getMessage() == null ? "Проверьте введённые данные." : e.getMessage());
        }
    }

    private String databaseMessage(SQLException e) {
        if ("23503".equals(e.getSQLState())) return "Выбранный клиент больше не существует. Закройте форму и выберите клиента заново.";
        if ("23505".equals(e.getSQLState())) return "Запись нарушает ограничение уникальности базы данных.";
        return e.getMessage();
    }

    @FXML private void handleCancel() { closeWindow(); }

    private Stage window() { return saveButton != null && saveButton.getScene() != null ? (Stage) saveButton.getScene().getWindow() : null; }
    private void closeWindow() { if (window() != null) window().close(); }
}
