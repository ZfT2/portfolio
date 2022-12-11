package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.util.function.Supplier;

import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.TaxonomyConfig;
import name.abuchen.portfolio.ui.views.taxonomy.DonutChartBuilder;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;

public class TaxonomyChartWidget extends CircularChartWidget<TaxonomyModel>
{
    private DonutChartBuilder builder = new DonutChartBuilder();

    public TaxonomyChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfigAfter(ReportingPeriodConfig.class, new TaxonomyConfig(this));
    }

    @Override
    protected void configureTooltip(CircularChartToolTip toolTip)
    {
        builder.configureToolTip(toolTip);
    }

    @Override
    public Supplier<TaxonomyModel> getUpdateTask()
    {
        return () -> {
            Taxonomy taxonomy = get(TaxonomyConfig.class).getTaxonomy();

            if (taxonomy != null)
            {
                Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter().filter(getClient());
                return new TaxonomyModel(getDashboardData().getExchangeRateProviderFactory(), filteredClient, taxonomy);
            }
            else
            {
                return null;
            }
        };
    }

    @Override
    protected void createCircularSeries(TaxonomyModel model)
    {
        if (model != null)
        {
            builder.createCircularSeries(getChart(), model);
        }
        else
        {
            ICircularSeries<?> circularSeries = (ICircularSeries<?>) getChart().getSeriesSet()
                            .createSeries(SeriesType.DOUGHNUT, Messages.LabelErrorNoTaxonomySelected);
            circularSeries.setBorderColor(Colors.WHITE);
            circularSeries.setSeries(new String[] { Messages.LabelErrorNoTaxonomySelected }, new double[] { 100 });
            circularSeries.setColor(Messages.LabelErrorNoTaxonomySelected, Colors.LIGHT_GRAY);
        }
    }
}