package org.thunder.mpdcontrol.helpers;

import java.util.ArrayList;
import java.util.List;

public class RadioStore 
{

	private static RadioStore instance = new RadioStore();
	public static RadioStore getInstance() { return instance; }
	private boolean bInitialized;
	
	private List<RadioItem> list = new ArrayList<RadioItem>();
	
	public RadioStore()
	{
		super();
		this.bInitialized = false;
	}
	
	public void init()
	{
		if (this.bInitialized) return;

		list.add(new RadioItem("Chic Radio programme vintage", "http://vintage.chicradio.fr:8000", "chic_radio_vintage"));
		
		list.add(new RadioItem("Fip", "http://mp3lg.tdf-cdn.com/fip/all/fiphautdebit.mp3", "fip"));
		
		list.add(new RadioItem("RTL", "http://streaming.radio.rtl.fr/rtl-1-44-96", "rtl_96"));
		list.add(new RadioItem("RTL2", "http://streaming.radio.rtl2.fr/rtl2-1-48-192", "rtl2_192"));
		
		
		this.bInitialized = true;
	}
	
	public int getCount()
	{
		return list.size();
	}
	
	public RadioItem getAt(int position)
	{
		return list.get(position);
	}
	
	public RadioItem defaultRadio()
	{
		return new RadioItem("Stream", "", "no_logo");
	}
	
	public RadioItem findUrl(String url)
	{
		for (RadioItem r : list) 
        {
			if (r.getUrl().equals(url)) return r;
        }
		return null;
	}
	
}
