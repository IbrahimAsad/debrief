package com.planetmayo.debrief.satc.gwt.client.contributions;

import java.beans.PropertyChangeEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;
import com.planetmayo.debrief.satc.model.contributions.BaseContribution;

public class AnalysisContributionView extends BaseContributionView
{

	interface AnalysisContributionViewUiBinder extends
			UiBinder<Widget, AnalysisContributionView>
	{
	}

	private static AnalysisContributionViewUiBinder uiBinder = GWT
			.create(AnalysisContributionViewUiBinder.class);

	private BaseContribution _myData;

	public AnalysisContributionView()
	{
		initWidget(uiBinder.createAndBindUi(this));
		initHandlers();
	}

	@Override
	protected BaseContribution getData()
	{
		return _myData;
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0)
	{
		super.propertyChange(arg0);
	}

	@Override
	public void setData(BaseContribution contribution)
	{
		super.setData(contribution);
		_myData = contribution;
	}

}
