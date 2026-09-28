package ru.mirea.appliancerepair.util;

import ru.mirea.appliancerepair.model.RepairRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DataExporter {
    void export(List<RepairRequest> requests, Path outputPath) throws IOException;
}
