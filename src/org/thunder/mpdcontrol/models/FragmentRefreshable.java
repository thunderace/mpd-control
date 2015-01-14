package org.thunder.mpdcontrol.models;


public class FragmentRefreshable extends Fragment implements InterfaceFragmentRefreshable
{
	private boolean refreshDone = false;

	@Override
    public void onStart() 
    {
		super.onStart();
		if (app.oMPDAsyncHelper.oMPD.isConnected()) 
		{
			if (!refreshDone) updateList();
		}
    }
	
	public void invalidateRefresh()
    {
		refreshDone = false;
    }
			
	@Override
	public void onDisplay() 
	{
		super.onDisplay();
		if (!refreshDone) updateList();
	}

	@Override
	public void updateList() 
	{
		refreshDone = true;
	}
	
}
