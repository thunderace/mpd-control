package org.thunder.mpdcontrol;

import java.io.IOException;
import java.net.InetAddress;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import org.thunder.jmdns.JmDNS;
import org.thunder.jmdns.ServiceEvent;
import org.thunder.jmdns.ServiceInfo;
import org.thunder.jmdns.ServiceListener;

public class ServiceBonjour extends Service implements ServiceListener 
{
	protected static final String MPD_SERVICE = "_mpd._tcp.local.";
	protected static final String TAG = MainApplication.TAG;
    
	private MulticastLock lock = null;
    private JmDNS jmdns = null;
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i(TAG, "BONJOUR onStartCommand");
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		try
        {
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifi.getConnectionInfo();
			
			int intaddr = wifiInfo.getIpAddress();
			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
			InetAddress addr=InetAddress.getByAddress(byteaddr);
		    String hostname = InetAddress.getByName(addr.getHostName()).toString();
			
			lock = wifi.createMulticastLock("mpdcontrol_bonjour");
	        lock.setReferenceCounted(true);
	        lock.acquire();
			jmdns = JmDNS.create(addr, hostname);
			jmdns.addServiceListener(MPD_SERVICE, this);
		} 
        catch (IOException e) 
        {
        	Log.e(TAG, e.getMessage());
		}
        
        return super.onStartCommand(intent, flags, startId);
	}
		
	@Override
	public void onDestroy()
	{
		Log.i(TAG, "BONJOUR onDestroy");
		
		if (jmdns != null)
		{
			jmdns.removeServiceListener(MPD_SERVICE, this);
			try { jmdns.close(); } catch (IOException e) {}
			jmdns = null;
		}
	    
		if (lock != null) 
		{
			lock.release();
			lock = null;
		}
				
        super.onDestroy();		
	}
			
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}

	@Override
	public void serviceAdded(ServiceEvent event) 
	{
		ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName());
		InetAddress[] addresses = info.getInetAddresses();
		if (addresses[0] != null)
		{
			String name = info.getName();
        	String host = addresses[0].toString();
        	int port = info.getPort();
        	Log.d(TAG, "Discovery: serviceAdded :" + String.format("%s, %s, %d", name, host, port));
		}
	}

	@Override
	public void serviceRemoved(ServiceEvent event) 
	{
	}

	@Override
	public void serviceResolved(ServiceEvent event) 
	{
	}
	
	public static ServiceInfo discoverService(String service, int timeOut, Context context) throws IOException
	{
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = manager.getConnectionInfo();
		
		int intaddr = wifiInfo.getIpAddress();
		byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
		InetAddress addr=InetAddress.getByAddress(byteaddr);
	    String hostname = InetAddress.getByName(addr.getHostName()).toString();
		
	    MulticastLock lock = manager.createMulticastLock("mpdcontrol_bonjour");
        lock.setReferenceCounted(true);
        lock.acquire();
        JmDNS jmdns = JmDNS.create(addr, hostname);
		
        ServiceInfo[] infos = {};
		infos = jmdns.list(service, timeOut);
				
		jmdns.close();
		
		if (infos.length == 0) return null;
		return infos[0];
	}
		
}
