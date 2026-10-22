package name.abuchen.portfolio.model;

import java.time.LocalDateTime;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    /**
     * Sets the booking date for both transactions of the entry.
     */
    void setBookingDate(LocalDateTime bookingDate);

    TransactionOwner<? extends Transaction> getOwner(Transaction t);

    void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner);

    Transaction getCrossTransaction(Transaction t);

    TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    void insert();

    String getSource();

    void setSource(String source);
}
