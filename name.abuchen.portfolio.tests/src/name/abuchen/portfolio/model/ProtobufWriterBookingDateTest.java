package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ProtobufWriterBookingDateTest
{
    @Test
    public void testBookingDateIsPreserved() throws IOException
    {
        var client = new Client();

        var account = new Account();
        account.setName("Test Account");
        client.addAccount(account);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        client.addPortfolio(portfolio);

        var security = new Security();
        security.setIsin("DE0007164600");
        security.setName("SAP SE");
        security.setCurrencyCode("EUR");
        client.addSecurity(security);

        // deposit with a booking date
        var deposit = new AccountTransaction();
        deposit.setType(AccountTransaction.Type.DEPOSIT);
        deposit.setDateTime(LocalDateTime.of(2013, 1, 1, 0, 0));
        deposit.setBookingDate(LocalDateTime.of(2012, 12, 31, 0, 0));
        deposit.setMonetaryAmount(Money.of("EUR", 10_000));
        account.addTransaction(deposit);

        // purchase with a booking date
        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setDate(LocalDateTime.of(2013, 1, 2, 0, 0));
        entry.setBookingDate(LocalDateTime.of(2013, 1, 1, 0, 0));
        entry.setMonetaryAmount(Money.of("EUR", 10_000));
        entry.setShares(10);
        entry.setCurrencyCode("EUR");
        entry.insert();

        // delivery without a booking date
        var delivery = new PortfolioTransaction();
        delivery.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        delivery.setDateTime(LocalDateTime.of(2013, 1, 3, 0, 0));
        delivery.setMonetaryAmount(Money.of("EUR", 10_000));
        delivery.setShares(10);
        delivery.setCurrencyCode("EUR");
        delivery.setSecurity(security);
        portfolio.addTransaction(delivery);

        var newClient = roundTrip(client);

        var accountTransactions = newClient.getAccounts().get(0).getTransactions();
        assertThat(accountTransactions.size(), is(2));

        var newDeposit = (AccountTransaction) accountTransactions.stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT).findFirst().orElseThrow();
        assertThat(newDeposit.getDateTime(), is(LocalDateTime.of(2013, 1, 1, 0, 0)));
        assertThat(newDeposit.getBookingDate(), is(LocalDateTime.of(2012, 12, 31, 0, 0)));

        var newPurchase = (AccountTransaction) accountTransactions.stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.BUY).findFirst().orElseThrow();
        assertThat(newPurchase.getBookingDate(), is(LocalDateTime.of(2013, 1, 1, 0, 0)));

        var portfolioTransactions = newClient.getPortfolios().get(0).getTransactions();
        assertThat(portfolioTransactions.size(), is(2));

        var newPortfolioTransaction = (PortfolioTransaction) portfolioTransactions.stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY).findFirst().orElseThrow();
        assertThat(newPortfolioTransaction.getBookingDate(), is(LocalDateTime.of(2013, 1, 1, 0, 0)));

        var newDelivery = (PortfolioTransaction) portfolioTransactions.stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND).findFirst().orElseThrow();
        assertThat(newDelivery.getBookingDate(), is(nullValue()));
    }

    private Client roundTrip(Client client) throws IOException
    {
        var protobufWriter = new ProtobufWriter();

        var stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        return protobufWriter.load(new ByteArrayInputStream(stream.toByteArray()));
    }
}
