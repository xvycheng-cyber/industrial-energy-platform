package com.smarten.module.datalake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarten.module.datalake.dto.IngestBatchDTO;
import com.smarten.module.datalake.dto.ReadingDTO;
import com.smarten.module.datalake.entity.DataIngestFailure;
import com.smarten.module.datalake.mapper.DataIngestFailureMapper;
import com.smarten.module.datalake.vo.ImportResultVO;
import com.smarten.module.datalake.vo.IngestResultVO;
import com.smarten.module.masterdata.entity.EnergyMeter;
import com.smarten.module.masterdata.mapper.EnergyMeterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel / CSV 导入。
 *
 * <p>文件列顺序：仪表编号、采集时间、读数、读数类型（可选，1 累计 2 增量）。
 * 解析失败与入库失败均记录到 data_ingest_failure，不丢弃任何行。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatalakeImportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnergyMeterMapper meterMapper;
    private final DataIngestFailureMapper failureMapper;
    private final DatalakeIngestService ingestService;

    public ImportResultVO importFile(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String batchNo = "IMP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        ImportResultVO result = new ImportResultVO();
        result.setBatchNo(batchNo);

        List<String[]> rows;
        try {
            rows = filename.endsWith(".csv") ? readCsv(file) : readExcel(file);
        } catch (Exception ex) {
            throw new com.smarten.common.exception.BusinessException(
                    com.smarten.common.api.ResultCode.BAD_REQUEST, "文件解析失败：" + ex.getMessage());
        }

        List<ReadingDTO> readings = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row == null || row.length < 3 || isBlank(row[0])) {
                continue;
            }
            total++;
            try {
                String meterCode = row[0].trim();
                EnergyMeter meter = meterMapper.selectOne(new LambdaQueryWrapper<EnergyMeter>()
                        .eq(EnergyMeter::getMeterCode, meterCode));
                if (meter == null) {
                    throw new IllegalArgumentException("仪表编号不存在：" + meterCode);
                }
                LocalDateTime collectTime = parseTime(row[1]);
                BigDecimal value = new BigDecimal(row[2].trim());
                Integer readingType = row.length > 3 && !isBlank(row[3])
                        ? Integer.valueOf(row[3].trim().replace(".0", "")) : meter.getReadingType();

                ReadingDTO reading = new ReadingDTO();
                reading.setMeterId(meter.getId());
                reading.setCollectTime(collectTime);
                reading.setValue(value);
                reading.setReadingType(readingType);
                reading.setPointCode("TOTAL");
                readings.add(reading);
            } catch (Exception ex) {
                String message = "第 " + (i + 2) + " 行：" + ex.getMessage();
                if (result.getErrors().size() < 50) {
                    result.getErrors().add(message);
                }
                recordFailure(batchNo, message);
                result.setFailedRows(result.getFailedRows() + 1);
            }
        }

        result.setTotalRows(total);
        if (!readings.isEmpty()) {
            IngestBatchDTO dto = new IngestBatchDTO();
            dto.setBatchNo(batchNo);
            dto.setSource("IMPORT");
            dto.setReadings(readings);
            IngestResultVO ingestResult = ingestService.ingest(dto);
            result.setSuccessRows(ingestResult.getInsertedCount());
            result.setFailedRows(result.getFailedRows() + ingestResult.getFailedCount());
        }
        return result;
    }

    private List<String[]> readCsv(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.toLowerCase().contains("meter") || line.contains("仪表")) {
                        continue;
                    }
                }
                if (line.isBlank()) {
                    continue;
                }
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    private List<String[]> readExcel(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean first = true;
            for (Row row : sheet) {
                if (first) {
                    first = false;
                    String head = formatter.formatCellValue(row.getCell(0));
                    if (head.toLowerCase().contains("meter") || head.contains("仪表")) {
                        continue;
                    }
                }
                List<String> values = new ArrayList<>();
                for (int c = 0; c < 4; c++) {
                    Cell cell = row.getCell(c);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                if (values.get(0).isBlank() && values.get(1).isBlank()) {
                    continue;
                }
                rows.add(values.toArray(new String[0]));
            }
        }
        return rows;
    }

    private LocalDateTime parseTime(String text) {
        String value = text.trim().replace("/", "-");
        if (value.length() == 16) {
            value = value + ":00";
        }
        return LocalDateTime.parse(value, FORMATTER);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void recordFailure(String batchNo, String message) {
        DataIngestFailure failure = new DataIngestFailure();
        failure.setBatchNo(batchNo);
        failure.setErrorCode("IMPORT_PARSE");
        failure.setErrorMsg(message.length() > 400 ? message.substring(0, 400) : message);
        failure.setRetryCount(0);
        failure.setStatus(0);
        failureMapper.insert(failure);
    }
}
