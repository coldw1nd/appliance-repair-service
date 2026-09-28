package ru.mirea.appliancerepair.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.appliancerepair.model.RepairRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelExporter implements DataExporter {
    @Override
    public void export(List<RepairRequest> requests, Path outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(outputPath)) {
            Sheet sheet = workbook.createSheet("Repair Requests");
            String[] headers = {"ID", "Customer", "Appliance Type", "Brand", "Issue", "Status", "Priority", "Estimated Cost", "Planned Date", "Urgent", "Created At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (RepairRequest request : requests) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(request.getId());
                row.createCell(1).setCellValue(request.getCustomerName());
                row.createCell(2).setCellValue(request.getApplianceType());
                row.createCell(3).setCellValue(request.getApplianceBrand());
                row.createCell(4).setCellValue(request.getIssueTitle());
                row.createCell(5).setCellValue(request.getStatus().name());
                row.createCell(6).setCellValue(request.getPriority().name());
                row.createCell(7).setCellValue(request.getEstimatedCost().doubleValue());
                row.createCell(8).setCellValue(request.getPlannedDate() == null ? "" : request.getPlannedDate().toString());
                row.createCell(9).setCellValue(request.isUrgent());
                row.createCell(10).setCellValue(request.getCreatedAt().toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
        }
    }
}
