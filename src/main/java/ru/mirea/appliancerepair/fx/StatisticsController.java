package ru.mirea.appliancerepair.fx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.service.CustomerService;
import ru.mirea.appliancerepair.service.RepairRequestService;
import ru.mirea.appliancerepair.util.Ui;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class StatisticsController {
    @FXML private Label totalRequestsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label newRequestsLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private Label urgentLabel;
    @FXML private Label averageCostLabel;
    @FXML private Label totalCostLabel;

    public void configure(RepairRequestService requestService, CustomerService customerService) throws SQLException {
        List<RepairRequest> requests = requestService.findAll();
        int customers = customerService.findAll().size();
        long newCount = requests.stream().filter(r -> r.getStatus() == RequestStatus.NEW).count();
        long inProgressCount = requests.stream().filter(r -> r.getStatus() == RequestStatus.DIAGNOSING || r.getStatus() == RequestStatus.REPAIRING).count();
        long completedCount = requests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
        long urgentCount = requests.stream().filter(RepairRequest::isUrgent).count();
        BigDecimal totalCost = requests.stream().map(RepairRequest::getEstimatedCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageCost = requests.isEmpty() ? BigDecimal.ZERO : totalCost.divide(BigDecimal.valueOf(requests.size()), 2, java.math.RoundingMode.HALF_UP);
        totalRequestsLabel.setText(Integer.toString(requests.size()));
        totalCustomersLabel.setText(Integer.toString(customers));
        newRequestsLabel.setText(Long.toString(newCount));
        inProgressLabel.setText(Long.toString(inProgressCount));
        completedLabel.setText(Long.toString(completedCount));
        urgentLabel.setText(Long.toString(urgentCount));
        averageCostLabel.setText(money(averageCost));
        totalCostLabel.setText(money(totalCost));
    }

    private String money(BigDecimal amount) { return String.format(Locale.forLanguageTag("ru-RU"), "%,.2f ₽", amount); }

    @FXML private void handleClose() {
        if (totalRequestsLabel != null && totalRequestsLabel.getScene() != null) {
            ((Stage) totalRequestsLabel.getScene().getWindow()).close();
        }
    }
}
