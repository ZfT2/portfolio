package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BawagAGPDFExtractor extends AbstractPDFExtractor
{
    public BawagAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BAWAG AG Niederlassung Deutschland");

        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "BAWAG AG";
    }

    private void addCreditcardStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Beleg- Buchungs-/ Beschreibung Karte Betrag (EUR)
                                        // Gebucht am Wertstellung am Verwendungszweck Betrag (EUR)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.* \\((?<currency>[A-Z]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))) //

                                        // In the first variant of the statement the first
                                        // column is the receipt date (Belegdatum) and the
                                        // booking date is the second column
                                        // (Buchungs-/Valutadatum). Only in the second
                                        // variant the first column is the booking date.
                                        // @formatter:off
                                        // Gebucht am Wertstellung am Verwendungszweck Betrag (EUR)
                                        // @formatter:on
                                        .section("bookedOnFirstDate").optional() //
                                        .match("^Gebucht am Wertstellung am .*$") //
                                        .assign((ctx, v) -> ctx.put("bookedOnFirstDate", "true")));

        this.addDocumentTyp(type);

        // @formatter:off
        // Beleg- Buchungs-/ Beschreibung Karte Betrag (EUR)
        // 23.12.2023 25.12.2023 Lidl sagt Danke        Ort           DE Visa B A             60,78-
        // 23.12.2023 28.12.2023 Lidl sagt Danke        Ort           DE Visa B A             60,78+
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(?!(Abgeltungsteuer|Solidarit.tszuschlag)).* [\\.,\\d]+[\\-|\\+]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date1", "date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .documentContextOptionally("bookedOnFirstDate") //
                        .match("^(?<date1>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>.{1,36})" //
                                        + ".* " //
                                        + "(?<amount>[\\.,\\d]+)" //
                                        + "(?<type>[\\-|\\+])$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));

                            // The booking date is the first column only in the second
                            // variant of the statement. Otherwise it is the second
                            // column (Buchungs-/Valutadatum).
                            if (v.get("bookedOnFirstDate") != null)
                                t.setBookingDate(asDate(v.get("date1")));
                            else
                                t.setBookingDate(asDate(v.get("date")));

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(replaceMultipleBlanks(v.get("note"))));
                        })

                        .wrap(TransactionItem::new));
    }
}
