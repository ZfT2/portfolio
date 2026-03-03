package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.widgets.Control;

public class NullableDateTimeDateSelectionProperty extends SimpleDateTimeDateSelectionProperty
{

    @Override
    protected void doSetValue(Control source, LocalDate date)
    {
        CDateTime dateTime = (CDateTime) source;

        if (date != null)
            dateTime.setSelection(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        else
            dateTime.setSelection(null);
    }
}
