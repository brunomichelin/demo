package com.example.demo.services.impl;

import com.example.demo.dtos.RelatorioCustoEtapaResponse;
import com.example.demo.dtos.RelatorioTarefasResponse;
import com.example.demo.models.StatusCronEnum;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioExportServiceTest {
    private final RelatorioExportService service = new RelatorioExportService();

    @Test
    void exportaRelatorioComoXlsxValido() throws Exception {
        var tarefaCounts = new RelatorioCustoEtapaResponse.Tarefas(1, 0, 0, 0, 1);
        var linha = new RelatorioCustoEtapaResponse.Etapa(1L, 1L, "Fundação", StatusCronEnum.CONCLUIDA,
                new BigDecimal("100.00"), new BigDecimal("104.00"), new BigDecimal("4.00"),
                new BigDecimal("4.00"), tarefaCounts, new BigDecimal("100.00"), true);
        var report = new RelatorioCustoEtapaResponse(1L, "Obra", null, 2L, List.of(linha),
                new RelatorioCustoEtapaResponse.Totais(new BigDecimal("100.00"), new BigDecimal("104.00"), 1));

        byte[] arquivo = service.custoXlsx(report);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            assertEquals("Custos por etapa", workbook.getSheetAt(0).getSheetName());
            assertEquals("Fundação", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertTrue(workbook.getSheetAt(0).getRow(1).getCell(9).getBooleanCellValue());
        }
    }

    @Test
    void exportaRelatorioPdfComAssinaturaPdf() {
        byte[] arquivo = service.tarefasPdf(List.of(
                new RelatorioTarefasResponse(1L, "Fundação", 0, 0, 0, 0, 0, 0)));
        assertTrue(new String(arquivo, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
    }
}
