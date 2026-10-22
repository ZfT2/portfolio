package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Transaction;

public class BookingDateEditingSupport extends OptionalDateEditingSupport
{
    @Override
    public boolean canEdit(Object element)
    {
        return Adaptor.adapt(Transaction.class, element) != null;
    }

    @Override
    protected LocalDateTime getDate(Transaction transaction)
    {
        return transaction.getBookingDate();
    }

    @Override
    protected void setDate(Transaction transaction, LocalDateTime date)
    {
        var crossEntry = transaction.getCrossEntry();
        if (crossEntry != null)
        {
            // keep both transactions of the entry in sync
            crossEntry.setBookingDate(date);
        }
        else
        {
            transaction.setBookingDate(date);
        }
    }
}
