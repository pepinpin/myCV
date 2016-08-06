package net.biospherecorp.mycv;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class FullscreenActivity extends AppCompatActivity {

	private static Handler handler;

	private static final String MAIN_FR = "http://www.biospherecorp.net/index-fr.php";
	private static final String MAIN_EN = "http://www.biospherecorp.net/index-en.php";
	private static final String ERROR_URL = "file:///android_asset/error.html";

	private String urlToLoad;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setup the activity
		setContentView(R.layout.activity_fullscreen);
		final WebView webView = (WebView) findViewById(R.id.webview);


		// get the system language
		String localeLanguage = Locale.getDefault().getISO3Language();

		// if it's french, load the french page
		// otherwise load the english one
		if (localeLanguage.equals("fra")){
			urlToLoad = MAIN_FR;
		}else{
			urlToLoad = MAIN_EN;
		}

		// uses the viewport HTML tag (if exists) to correctly display webpages
		webView.getSettings().setUseWideViewPort(true);

		// set the web client and intercept all links clicked
		// if the link is redirecting to a page outside the biospherecorp.net
		// domain, open it in a new browser
		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {

				if (url != null &&
						!url.startsWith("http://www.biospherecorp.net")){
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
					return  true;
				}

				return false;
			}
		});

		// instantiate the handler
		// for cross thread communication
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {

				// if there is an internet connection,
				// load the website
				if (msg.arg1 == 1){
					webView.loadUrl(urlToLoad);

				}else{
					//if not, load the error HTML page
					// from the Assets folder
					webView.loadUrl(ERROR_URL);
				}
			}
		};

		// create and run the internet test
		// load the main page if Internet OK
		// load error page otherwise
		Thread testInternet = new Thread(new TestInternet());
		testInternet.start();
	}


	// Network operations CANNOT be executed in the main thread
	private class TestInternet implements Runnable {

		@Override
		public void run() {

			// get a message
			Message message = Message.obtain();

			// check first to see if there is a network available
			if (isNetworkAvailable()) {
				try {
					// try to connect to the google special page for tests
					HttpURLConnection connection = (HttpURLConnection)
							(new URL("http://clients3.google.com/generate_204")
									.openConnection());

					// set the request properties
					connection.setRequestProperty("User-Agent", "Android");
					connection.setRequestProperty("Connection", "close");
					connection.setConnectTimeout(1500);

					// start the connection
					connection.connect();

					// if everything went well => no exception,
					// ie: there is an internet connection
					// AND if the response is 204
					if (connection.getResponseCode() == 204 &&
							connection.getContentLength() == 0){

						// prepare a message for the main thread
						// to say that there is an internet connection
						message.arg1 = 1; // 1 means OK

					}

				// if there is an error
				} catch (IOException e) {

					// prepare a message for the main thread
					// to say that there is NO internet connection
					message.arg1 = 0; // 0 means ERROR
				}finally {

					//send the message
					handler.sendMessage(message);
				}

			} else {

				// prepare & send a message to the main thread
				// to say that there is NO internet connection
				message.arg1 = 0; // 0 means ERROR
				handler.sendMessage(message);
			}
		}

		private boolean isNetworkAvailable(){

			// get a NetworkInfo object from the connectivityManager
			// and check to see if it's null or not
			//
			// need a permission in the manifest : "ACCESS_NETWORK_STATE"
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			return connManager.getActiveNetworkInfo() != null;
		}
	}
}
