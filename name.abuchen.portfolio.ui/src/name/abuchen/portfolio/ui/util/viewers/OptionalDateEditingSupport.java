package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

/**
 * Base class for editing supports that let the user enter an optional date, for
 * example the ex-date or the booking date. If no date is entered, the value is
 * removed from the transaction.
 */
public abstract class OptionalDateEditingSupport extends ColumnEditingSupport
{
    private static final DateTimeFormatter[] dateFormatters = new DateTimeFormatter[] {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), //
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), //
                    DateTimeFormatter.ofPattern("d.M.yyyy"), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                    DateTimeFormatter.ISO_DATE };

    /**
     * Returns the date of the given transaction or {@code null} if the
     * transaction does not have the date.
     */
    protected abstract LocalDateTime getDate(Transaction transaction);

    /**
     * Sets the date of the given transaction. A {@code null} value removes the
     * date.
     */
    protected abstract void setDate(Transaction transaction, LocalDateTime date);

    @Override
    public Object getValue(Object element)
    {
        var tx = Adaptor.adapt(Transaction.class, element);
        var date = tx != null ? getDate(tx) : null;
        return date != null ? Values.Date.format(date.toLocalDate()) : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value)
    {
        var tx = Adaptor.adapt(Transaction.class, element);
        if (tx == null)
            return;

        var oldValue = getDate(tx);

        var inputValue = ((String) value).trim();

        // the date is an optional value, therefore we remove it if no input is
        // given
        if (inputValue.isEmpty())
        {
            if (oldValue != null)
            {
                setDate(tx, null);
                notify(element, null, oldValue);
            }
            return;
        }

        LocalDateTime newValue = null;
        for (DateTimeFormatter formatter : dateFormatters)
        {
            try
            {
                newValue = LocalDate.parse(inputValue, formatter).atStartOfDay();
                break;
            }
            catch (DateTimeParseException ignore)
            {
                // continue with next formatter
            }
        }

        // there is text input, but it cannot be parsed as a local date -> throw
        // an error
        if (newValue == null)
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value));

        if (!newValue.equals(oldValue))
        {
            setDate(tx, newValue);
            notify(element, newValue, oldValue);
        }
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(20);
        return textEditor;
    }
}
