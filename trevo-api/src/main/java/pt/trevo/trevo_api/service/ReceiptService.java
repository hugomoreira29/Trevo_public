package pt.trevo.trevo_api.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import pt.trevo.trevo_api.dto.ReceiptData;
import pt.trevo.trevo_api.dto.ReceiptLineItem;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderItem;

@Service
public class ReceiptService {

    public ReceiptData buildReceipt(Order order) {
        List<ReceiptLineItem> items = order.getItems().stream()
            .map(this::toLineItem)
            .toList();

        return new ReceiptData(
            order.getId(),
            order.getBuyer().getFullName(),
            order.getBuyer().getEmail(),
            order.getCreatedAt(),
            items,
            order.getTotalAmount()
        );
    }

    public byte[] generatePdf(ReceiptData receipt) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Receipt #" + receipt.orderId()));
            doc.add(new Paragraph("Date: " + receipt.issuedAt()));
            doc.add(new Paragraph("Buyer: " + receipt.buyerName() + " <" + receipt.buyerEmail() + ">"));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.addCell("Product");
            table.addCell("Qty");
            table.addCell("Unit Price (EUR)");
            table.addCell("Line Total (EUR)");

            for (ReceiptLineItem item : receipt.items()) {
                String label = item.productName();
                if (item.size() != null) label += " / " + item.size();
                if (item.color() != null) label += " / " + item.color();
                table.addCell(label);
                table.addCell(String.valueOf(item.quantity()));
                table.addCell(item.unitPrice().toPlainString());
                table.addCell(item.lineTotal().toPlainString());
            }

            doc.add(table);
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Total: EUR " + receipt.total().toPlainString()));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private ReceiptLineItem toLineItem(OrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new ReceiptLineItem(
            item.getVariant().getProduct().getName(),
            item.getVariant().getSize(),
            item.getVariant().getColor(),
            item.getQuantity(),
            item.getUnitPrice(),
            lineTotal
        );
    }
}
