package ru.mirea.appliancerepair.fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.appliancerepair.exception.BusinessException;
import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.service.CustomerService;
import ru.mirea.appliancerepair.util.Ui;

import java.sql.SQLException;

public class CustomerDialogController {
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Long> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> addressColumn;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;

    private CustomerService customerService;

    public void configure(CustomerService customerService) throws SQLException {
        this.customerService = customerService;
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getFullName()));
        phoneColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getPhone()));
        emailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEmail()));
        addressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getAddress()));
        refresh();
    }

    @FXML
    private void handleAdd() {
        try {
            Customer customer = new Customer(text(nameField), text(phoneField), text(emailField), text(addressField));
            customerService.create(customer);
            clearForm();
            refresh();
            Ui.info(owner(), "Клиент добавлен", "Клиент «" + customer.getFullName() + "» сохранён в PostgreSQL.");
        } catch (BusinessException e) {
            Ui.error(owner(), "Проверьте данные клиента", e.getMessage());
        } catch (SQLException e) {
            Ui.error(owner(), "Не удалось добавить клиента", e.getMessage());
        }
    }

    @FXML private void handleRefresh() {
        try { refresh(); }
        catch (SQLException e) { Ui.error(owner(), "Ошибка обновления", e.getMessage()); }
    }

    private void refresh() throws SQLException {
        customerTable.setItems(FXCollections.observableArrayList(customerService.findAll()));
    }

    private String text(TextField field) { return field.getText() == null ? "" : field.getText().trim(); }

    private void clearForm() {
        nameField.clear(); phoneField.clear(); emailField.clear(); addressField.clear();
    }

    @FXML private void handleClose() { if (owner() != null) owner().close(); }
    private Stage owner() { return customerTable != null && customerTable.getScene() != null ? (Stage) customerTable.getScene().getWindow() : null; }
}
