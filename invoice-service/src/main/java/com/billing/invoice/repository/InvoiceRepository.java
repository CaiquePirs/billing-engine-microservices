package com.billing.invoice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.billing.invoice.model.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

}
