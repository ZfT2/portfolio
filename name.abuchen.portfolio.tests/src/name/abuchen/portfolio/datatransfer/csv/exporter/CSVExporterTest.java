package name.abuchen.portfolio.datatransfer.csv.exporter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityUpdateItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class CSVExporterTest
{
    @Test
    public void testBookingDateRoundTrip() throws Exception
    {
        var client = new Client();
        var account = new Account();
        account.setName("Test Account");
        client.addAccount(account);

        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setDateTime(LocalDateTime.of(2013, 1, 1, 0, 0));
        transaction.setBookingDate(LocalDateTime.of(2012, 12, 31, 0, 0));
        transaction.setAmount(10_000);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        account.addTransaction(transaction);

        var file = File.createTempFile("export", ".csv");
        file.deleteOnExit();
        new CSVExporter().exportTransactions(file, account.getTransactions());

        // the exported file contains a booking date column
        var text = Files.readString(file.toPath());
        assertThat(text, containsString(Messages.CSVColumn_BookingDate));

        // re-import: the booking date is preserved
        var importer = new CSVImporter(client, file);
        importer.setEncoding(StandardCharsets.UTF_8);
        importer.setExtractor(importer.getExtractorByCode("account-transaction").orElseThrow());
        importer.processFile(true); // remap columns to fields
        var errors = new ArrayList<Exception>();
        var results = importer.createItems(errors);

        assertThat(errors, empty());

        var imported = results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .map(i -> (AccountTransaction) i.getSubject()).orElseThrow();

        assertThat(imported.getDateTime(), is(LocalDateTime.of(2013, 1, 1, 0, 0)));
        assertThat(imported.getBookingDate(), is(LocalDateTime.of(2012, 12, 31, 0, 0)));
    }

    @Test
    public void testBookingDateIsEmptyIfNotSet() throws Exception
    {
        var client = new Client();
        var account = new Account();
        account.setName("Test Account");
        client.addAccount(account);

        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setDateTime(LocalDateTime.of(2013, 1, 1, 0, 0));
        transaction.setAmount(10_000);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        account.addTransaction(transaction);

        var file = File.createTempFile("export", ".csv");
        file.deleteOnExit();
        new CSVExporter().exportTransactions(file, account.getTransactions());

        // re-import: no booking date is set
        var importer = new CSVImporter(client, file);
        importer.setEncoding(StandardCharsets.UTF_8);
        importer.setExtractor(importer.getExtractorByCode("account-transaction").orElseThrow());
        importer.processFile(true); // remap columns to fields
        var errors = new ArrayList<Exception>();
        var results = importer.createItems(errors);

        assertThat(errors, empty());

        var imported = results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .map(i -> (AccountTransaction) i.getSubject()).orElseThrow();

        assertThat(imported.getBookingDate(), is(nullValue()));
    }

    @Test
    public void testAttributeRoundTrip() throws Exception
    {
        var client = new Client();
        var ter = new AttributeType("ter2");
        ter.setName("TER2");
        ter.setColumnLabel("TER2");
        ter.setTarget(Security.class);
        ter.setType(Double.class);
        ter.setConverter(PercentConverter.class);
        client.getSettings().addAttributeType(ter);

        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        security.getAttributes().put(ter, 0.125d); // 12.5 %
        client.addSecurity(security);

        var file = File.createTempFile("export", ".csv");
        file.deleteOnExit();
        new CSVExporter().exportSecurityMasterData(client, file, client.getSecurities());

        // re-import: values identical -> no update items
        var importer = new CSVImporter(client, file);
        importer.setEncoding(StandardCharsets.UTF_8);
        importer.setExtractor(importer.getExtractorByCode("investment-vehicle").orElseThrow());
        importer.processFile(true); // remap columns to fields
        var errors = new ArrayList<Exception>();
        List<Item> results = importer.createItems(errors);

        assertThat(errors, empty());
        assertThat(results.stream().filter(SecurityUpdateItem.class::isInstance).count(), is(0L));

        // now change the TER value in the file and re-import -> one update
        // (derive the exact old/new tokens from the converter itself rather than
        // hard-coding "12.5"/"20" -- PercentConverter formatting is locale-dependent,
        // e.g. "12,50%" under a locale using a comma decimal separator)
        var oldToken = ter.getConverter().toString(0.125d);
        var newToken = ter.getConverter().toString(0.20d);
        var text = Files.readString(file.toPath()).replace(oldToken, newToken);
        Files.writeString(file.toPath(), text);

        var importer2 = new CSVImporter(client, file);
        importer2.setEncoding(StandardCharsets.UTF_8);
        importer2.setExtractor(importer2.getExtractorByCode("investment-vehicle").orElseThrow());
        importer2.processFile(true); // remap columns to fields
        var errors2 = new ArrayList<Exception>();
        List<Item> results2 = importer2.createItems(errors2);

        assertThat(errors2, empty());
        assertThat(results2.stream().filter(SecurityUpdateItem.class::isInstance).count(), is(1L));
    }
}
