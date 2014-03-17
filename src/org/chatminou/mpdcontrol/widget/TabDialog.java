package org.chatminou.mpdcontrol.widget;

import org.chatminou.mpdcontrol.R;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabWidget;

public class TabDialog extends Dialog 
{

	public TabDialog(Context context) 
	{
		super(context);
		setTitle("Enregistrer la liste de lecture");
				
		// no title on this dialog
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.tab_dialog);
		
		// instantiate our list views for each tab
        //ListView listView01 = (ListView)findViewById(R.id.listView01);
        //ListView listView02 = (ListView)findViewById(R.id.listView02);
		
		// register a context menu for all our listView02 items
        //registerForContextMenu(listView02);
		
		// instantiate and set our custom list view adapters
        //listView01Adapter = new ListView01Adapter(context);
        //listView01.setAdapter(listView01Adapter);
		
		/*listView01.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id)
            {                   
                // will dismiss the dialog
                dismiss();
            }           
        });*/
		
		
		// get our tabHost from the xml
        /*TabHost tabs = (TabHost)findViewById(R.id.tabhost);
        tabs.setup();
        
        // create tab 1
        TabHost.TabSpec tab1 = tabs.newTabSpec("tab1");
        tab1.setContent(R.id.ScrollView01);
        tab1.setIndicator("List 1");
        tabs.addTab(tab1);
        
     // create tab 2
        TabHost.TabSpec tab2 = tabs.newTabSpec("tab2");
        tab2.setContent(R.id.ScrollView02);
        tab2.setIndicator("List 01");
        tabs.addTab(tab2);*/
        
	}
	

	
	/*
	 *  // get our tabHost from the xml
        TabHost tabHost = (TabHost)findViewById(R.id.TabHost01);
        tabHost.setup();
        
        // create tab 1
        TabHost.TabSpec spec1 = tabHost.newTabSpec("tab1");
        spec1.setIndicator("Profile", context.getResources().getDrawable(R.drawable.tab_image1));
        spec1.setContent(R.id.TextView01);
        tabHost.addTab(spec1);
        //create tab2
        TabHost.TabSpec spec2 = tabHost.newTabSpec("tab2");
        spec2.setIndicator("Profile", context.getResources().getDrawable(R.drawable.tab_image2));
        spec2.setContent(R.id.TextView02);
        tabHost.addTab(spec2);
	 */
}
