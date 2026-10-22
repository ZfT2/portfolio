package name.abuchen.portfolio.ui.util.viewers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;

@SuppressWarnings("nls")
public class BookingDateEditingSupportTest
{
    @Test
    public void blankValueClearsBookingDateAndNotifiesNullAsNewValue()
    {
        var support = new BookingDateEditingSupport();

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DEPOSIT);
        tx.setBookingDate(LocalDateTime.of(2026, 2, 21, 10, 15));

        var element = new TransactionPair<>(new Account("Test Account"), tx);

        AtomicBoolean notified = new AtomicBoolean(false);
        AtomicReference<Object> newValue = new AtomicReference<>();
        AtomicReference<Object> oldValue = new AtomicReference<>();

        support.addListener((e, n, o) -> {
            notified.set(true);
            newValue.set(n);
            oldValue.set(o);
        });

        support.setValue(element, "   ");

        assertThat(tx.getBookingDate(), is(nullValue()));
        assertThat(notified.get(), is(true));
        assertThat(newValue.get(), is(nullValue()));
        assertThat(oldValue.get(), is(LocalDateTime.of(2026, 2, 21, 10, 15)));
        assertThat(support.getValue(element), is(""));
    }

    @Test
    public void parsedLocalDateIsStoredAsLocalDateTimeAtStartOfDay()
    {
        var support = new BookingDateEditingSupport();

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DEPOSIT);

        var element = new TransactionPair<>(new Account("Test Account"), tx);

        support.setValue(element, "2026-02-22");

        assertThat(tx.getBookingDate(), is(LocalDate.of(2026, 2, 22).atStartOfDay()));
    }

    @Test
    public void bothTransactionsOfCrossEntryAreUpdated()
    {
        var support = new BookingDateEditingSupport();

        var account = new Account("Test Account");
        account.setCurrencyCode("EUR");
        var portfolio = new Portfolio("Test Portfolio");

        var security = new Security();
        security.setName("SAP SE");
        security.setCurrencyCode("EUR");

        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.of(2026, 2, 22, 0, 0));
        entry.setSecurity(security);
        entry.setCurrencyCode("EUR");
        entry.setAmount(1000);
        entry.setShares(10);
        entry.insert();

        var element = new TransactionPair<>(portfolio, entry.getPortfolioTransaction());

        support.setValue(element, "2026-02-21");

        assertThat(entry.getPortfolioTransaction().getBookingDate(), is(LocalDate.of(2026, 2, 21).atStartOfDay()));
        assertThat(entry.getAccountTransaction().getBookingDate(), is(LocalDate.of(2026, 2, 21).atStartOfDay()));
    }
}
