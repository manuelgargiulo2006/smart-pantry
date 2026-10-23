package com.pantry.dto;

import com.pantry.model.Storage;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Righe dello scontrino riconosciute dall'AI, che l'utente controlla prima di importarle. */
public class ReceiptImportForm {

    private List<Row> rows = new ArrayList<>();

    public static class Row {
        private boolean include = true;
        private String receiptText;
        private Long productId;
        private Integer quantity = 1;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expirationDate;
        private Storage storage = Storage.DISPENSA;

        public boolean isInclude() {
            return include;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }

        public String getReceiptText() {
            return receiptText;
        }

        public void setReceiptText(String receiptText) {
            this.receiptText = receiptText;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }

        public Storage getStorage() {
            return storage;
        }

        public void setStorage(Storage storage) {
            this.storage = storage;
        }
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }
}
