package com.example.demo.services.impl;

import com.example.demo.dtos.RelatorioCustoEtapaResponse;
import com.example.demo.dtos.RelatorioInsumoResponse;
import com.example.demo.dtos.RelatorioTarefasResponse;
import com.example.demo.dtos.RelatorioPrazoEntregaResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class RelatorioExportService {

    public byte[] custoPdf(RelatorioCustoEtapaResponse relatorio) {
        return criarPdf("Relatório de custos por etapa", writer -> {
            Table tabela = new Table(6);
            adicionarCelula(tabela, "Etapa");
            adicionarCelula(tabela, "Orçado (R$)");
            adicionarCelula(tabela, "Realizado (R$)");
            adicionarCelula(tabela, "Desvio (R$)");
            adicionarCelula(tabela, "Desvio (%)");
            adicionarCelula(tabela, "Tarefas");
            for (var etapa : relatorio.etapas()) {
                adicionarCelula(tabela, etapa.nome());
                adicionarCelula(tabela, etapa.custoOrcado().toPlainString());
                adicionarCelula(tabela, etapa.custoRealizado().toPlainString());
                adicionarCelula(tabela, etapa.desvioValor().toPlainString());
                adicionarCelula(tabela, etapa.desvioPercentual() == null ? "-" : etapa.desvioPercentual().toPlainString());
                adicionarCelula(tabela, Integer.toString(etapa.tarefas().total()));
            }
            writer.add(tabela);
        });
    }

    public byte[] insumosPdf(List<RelatorioInsumoResponse> relatorio) {
        return criarPdf("Relatório de insumos por etapa", writer -> {
            Table tabela = new Table(6);
            adicionarCelula(tabela, "Etapa");
            adicionarCelula(tabela, "Insumo");
            adicionarCelula(tabela, "Qtd. orçada");
            adicionarCelula(tabela, "Qtd. realizada");
            adicionarCelula(tabela, "Valor orçado (R$)");
            adicionarCelula(tabela, "Valor realizado (R$)");
            relatorio.forEach(item -> {
                adicionarCelula(tabela, item.etapa());
                adicionarCelula(tabela, item.insumo());
                adicionarCelula(tabela, item.quantidadeOrcada().toPlainString());
                adicionarCelula(tabela, item.quantidadeRealizada().toPlainString());
                adicionarCelula(tabela, item.valorOrcado().toPlainString());
                adicionarCelula(tabela, item.valorRealizado().toPlainString());
            });
            writer.add(tabela);
        });
    }

    public byte[] tarefasPdf(List<RelatorioTarefasResponse> relatorio) {
        return criarPdf("Relatório de tarefas por etapa", writer -> {
            Table tabela = new Table(6);
            adicionarCelula(tabela, "Etapa");
            adicionarCelula(tabela, "Total");
            adicionarCelula(tabela, "A fazer");
            adicionarCelula(tabela, "Em andamento");
            adicionarCelula(tabela, "Bloqueadas");
            adicionarCelula(tabela, "Concluídas");
            relatorio.forEach(item -> {
                adicionarCelula(tabela, item.etapa());
                adicionarCelula(tabela, Integer.toString(item.total()));
                adicionarCelula(tabela, Integer.toString(item.aFazer()));
                adicionarCelula(tabela, Integer.toString(item.emAndamento()));
                adicionarCelula(tabela, Integer.toString(item.bloqueada()));
                adicionarCelula(tabela, Integer.toString(item.concluida()));
            });
            writer.add(tabela);
        });
    }

    public byte[] prazosPdf(List<RelatorioPrazoEntregaResponse> relatorio) {
        return criarPdf("Relatório de prazos de entrega", writer -> {
            Table tabela = new Table(5);
            adicionarCelula(tabela, "Etapa");
            adicionarCelula(tabela, "Data prevista");
            adicionarCelula(tabela, "Data real");
            adicionarCelula(tabela, "Atraso (dias)");
            adicionarCelula(tabela, "Situação");
            relatorio.forEach(item -> {
                adicionarCelula(tabela, item.etapa());
                adicionarCelula(tabela, item.dataPrevista() == null ? "-" : item.dataPrevista().toString());
                adicionarCelula(tabela, item.dataReal() == null ? "-" : item.dataReal().toString());
                adicionarCelula(tabela, item.diasAtraso() == null ? "-" : item.diasAtraso().toString());
                adicionarCelula(tabela, item.situacao() == null ? "PENDENTE" : item.situacao().name());
            });
            writer.add(tabela);
        });
    }

    public byte[] custoXlsx(RelatorioCustoEtapaResponse relatorio) {
        return criarXlsx("Custos por etapa", sheet -> {
            Row header = sheet.createRow(0);
            String[] colunas = {"Etapa", "Status", "Orçado (R$)", "Realizado (R$)", "Desvio (R$)",
                    "Desvio (%)", "Tarefas", "Concluídas", "Conclusão (%)", "Alerta 90%"};
            for (int i = 0; i < colunas.length; i++) {
                header.createCell(i).setCellValue(colunas[i]);
            }
            int rowIndex = 1;
            for (var etapa : relatorio.etapas()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(etapa.nome());
                row.createCell(1).setCellValue(etapa.status() == null ? "" : etapa.status().name());
                row.createCell(2).setCellValue(etapa.custoOrcado().doubleValue());
                row.createCell(3).setCellValue(etapa.custoRealizado().doubleValue());
                row.createCell(4).setCellValue(etapa.desvioValor().doubleValue());
                if (etapa.desvioPercentual() != null) {
                    row.createCell(5).setCellValue(etapa.desvioPercentual().doubleValue());
                }
                row.createCell(6).setCellValue(etapa.tarefas().total());
                row.createCell(7).setCellValue(etapa.tarefas().concluida());
                row.createCell(8).setCellValue(etapa.percentualConclusao().doubleValue());
                row.createCell(9).setCellValue(etapa.alertaOrcamento());
            }
        });
    }

    public byte[] insumosXlsx(List<RelatorioInsumoResponse> relatorio) {
        return criarXlsx("Insumos por etapa", sheet -> {
            String[] colunas = {"Etapa", "Insumo", "Tipo", "Unidade", "Qtd. orçada", "Qtd. realizada",
                    "Valor orçado (R$)", "Valor realizado (R$)"};
            cabecalho(sheet, colunas);
            int rowIndex = 1;
            for (var item : relatorio) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.etapa());
                row.createCell(1).setCellValue(item.insumo());
                row.createCell(2).setCellValue(item.tipo() == null ? "" : item.tipo());
                row.createCell(3).setCellValue(item.unidade() == null ? "" : item.unidade());
                row.createCell(4).setCellValue(item.quantidadeOrcada().doubleValue());
                row.createCell(5).setCellValue(item.quantidadeRealizada().doubleValue());
                row.createCell(6).setCellValue(item.valorOrcado().doubleValue());
                row.createCell(7).setCellValue(item.valorRealizado().doubleValue());
            }
        });
    }

    public byte[] tarefasXlsx(List<RelatorioTarefasResponse> relatorio) {
        return criarXlsx("Tarefas por etapa", sheet -> {
            String[] colunas = {"Etapa", "Total", "A fazer", "Em andamento", "Bloqueadas", "Concluídas",
                    "Conclusão (%)"};
            cabecalho(sheet, colunas);
            int rowIndex = 1;
            for (var item : relatorio) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.etapa());
                row.createCell(1).setCellValue(item.total());
                row.createCell(2).setCellValue(item.aFazer());
                row.createCell(3).setCellValue(item.emAndamento());
                row.createCell(4).setCellValue(item.bloqueada());
                row.createCell(5).setCellValue(item.concluida());
                row.createCell(6).setCellValue(item.percentualConclusao());
            }
        });
    }

    public byte[] prazosXlsx(List<RelatorioPrazoEntregaResponse> relatorio) {
        return criarXlsx("Prazos de entrega", sheet -> {
            cabecalho(sheet, new String[]{"Etapa", "Data prevista", "Data real", "Atraso (dias)", "Situação"});
            int rowIndex = 1;
            for (var item : relatorio) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.etapa());
                row.createCell(1).setCellValue(item.dataPrevista() == null ? "" : item.dataPrevista().toString());
                row.createCell(2).setCellValue(item.dataReal() == null ? "" : item.dataReal().toString());
                if (item.diasAtraso() != null) {
                    row.createCell(3).setCellValue(item.diasAtraso());
                }
                row.createCell(4).setCellValue(item.situacao() == null ? "PENDENTE" : item.situacao().name());
            }
        });
    }

    private byte[] criarPdf(String titulo, ConteudoPdf conteudo) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(titulo));
            conteudo.adicionar(document);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gerar o relatório PDF.", e);
        }
    }

    private byte[] criarXlsx(String nomePlanilha, ConteudoXlsx conteudo) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(nomePlanilha);
            conteudo.adicionar(sheet);
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gerar o relatório XLSX.", e);
        }
    }

    private void cabecalho(Sheet sheet, String[] colunas) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            header.createCell(i).setCellValue(colunas[i]);
        }
    }

    private void adicionarCelula(Table tabela, String texto) {
        try {
            tabela.addCell(texto);
        } catch (DocumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível montar o relatório PDF.", e);
        }
    }

    @FunctionalInterface
    private interface ConteudoPdf {
        void adicionar(Document document) throws DocumentException;
    }

    @FunctionalInterface
    private interface ConteudoXlsx {
        void adicionar(Sheet sheet);
    }
}
