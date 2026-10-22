package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDateTime;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Transaction;

public class ExDateEditingSupport extends OptionalDateEditingSupport
{
    @Override
    public boolean canEdit(Object element)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);
        return tx != null && tx.getSecurity() != null;
    }

    @Override
    protected LocalDateTime getDate(Transaction transaction)
    {
        return ((AccountTransaction) transaction).getExDate();
    }

    @Override
    protected void setDate(Transaction transaction, LocalDateTime date)
    {
        ((AccountTransaction) transaction).setExDate(date);
    }
}
