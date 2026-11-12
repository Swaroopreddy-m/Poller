package com.platform.module.document.generator;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.platform.common.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Streaming Document Generator.
 * Implements low-memory footprint streaming algorithms:
 * - PDF via OpenHTMLtoPDF
 * - Excel via Apache POI SXSSFWorkbook (sliding row window to avoid Heap exhaustion)
 * - CSV via Apache Commons CSV streaming printer
 */
@Slf4j
@Component
public class DocumentGeneratorEngine {

    public byte[] generatePdfFromHtml(String renderedHtml) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(renderedHtml, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed rendering PDF document", e);
            throw new PlatformException("PDF generation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GEN_ERROR");
        }
    }

    public byte[] generateStreamingExcel(List<String> headers, List<List<Object>> rows) {
        // Keep 100 rows in memory, flushing excess to disk temp files
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Data");
            
            // Write Header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            // Write Data Rows
            int rowIdx = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int colIdx = 0; colIdx < rowData.size(); colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object val = rowData.get(colIdx);
                    if (val instanceof Number num) {
                        cell.setCellValue(num.doubleValue());
                    } else if (val instanceof Boolean b) {
                        cell.setCellValue(b);
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            workbook.write(outputStream);
            workbook.dispose(); // Delete temporary disk files
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed generating Excel document", e);
            throw new PlatformException("Excel generation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "EXCEL_GEN_ERROR");
        }
    }

    public byte[] generateCsv(List<String> headers, List<List<Object>> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build())) {

            for (List<Object> row : rows) {
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed generating CSV document", e);
            throw new PlatformException("CSV generation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "CSV_GEN_ERROR");
        }
    }
}
