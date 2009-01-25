package org.rssowl.ui.internal.views.statistics;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.AttributedString;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.rssowl.core.service.StatisticsService;
import org.rssowl.ui.internal.Activator;

public class ReadPercentageChart{
	private StatisticsService fStatisticsService;
	
	public ReadPercentageChart() {
		fStatisticsService = new StatisticsService();
	}
	
	public JFreeChart getPieChart() {
		JFreeChart chart = ChartFactory.createPieChart("Overall read percentage", createDataSet(), true, true, false);
		PiePlot plot = (PiePlot)chart.getPlot();
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("No data available");
        plot.setCircular(false);
        plot.setLabelGap(0.02);
        plot.setLabelGenerator(new PieSectionLabelGenerator() {
			@SuppressWarnings("unchecked")
			public String generateSectionLabel(PieDataset dataSet, Comparable key) {
				return dataSet.getValue(key).toString() + " %";
			}

			@SuppressWarnings("unchecked")
			public AttributedString generateAttributedSectionLabel(PieDataset dataSet, Comparable key) {
				return null;
			}
        });
        return chart;
	}
	
	private PieDataset createDataSet() {
		double readPercentage = fStatisticsService.computeReadPercentage();
		DefaultPieDataset dataSet = new DefaultPieDataset();
		dataSet.setValue("Read news", readPercentage);
		dataSet.setValue("Unread news", 100 - readPercentage);
		return dataSet;
	}
	
	public Image createChartImage(JFreeChart chart, int width, int height) {
		Image image = null;
		
		try {
			// Write the image to a buffer in memory using AWT
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChartUtilities.writeChartAsPNG(out, chart, width, height);
			out.close();
		    
			// Load the image from the same buffer using SWT
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			image = new Image(Display.getCurrent(), in);
			in.close();
		} catch (Exception e) {
			Activator.getDefault().logError(e.getMessage(), e);
		} 
		   
		return image;
	} 
}
