/**
 * 
 */
package com.example.fudanbbs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.StringBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.example.fudanbbs.BoardActivity.TopicListAsyncTask;

import android.R.color;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Joseph.Zhong
 *
 */
public class ReplyTopicActivity extends Activity {
	private TextView TVBoardname;
	private EditText NewTopicTitle, ETEditText;
	private Button ButtonUpload, ButtonNewpost;
	private String board, boardURL, fid, bid, attachmentenability;
	private ArrayList<String> attachmenturlarraylist;
	private String TAG = "#########"+this.getClass().getName();
	private LinearLayout imagelayout;
	private ProgressDialog progressdialog;
	private AlertDialog.Builder builder;
	private ReplypostAsyncTask asynctask;
	private int attachmentmaxsize;
	private String owner, nick, title;
	private String replytext;
	private GetReplyPostPageAsyncTask getnewpostasynctask;
	private UploadAttachmentAsyncTask uploadattachementasynctask;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newtopic);
		
		ActionBar actionbar = getActionBar();
		actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setIcon(new ColorDrawable(color.transparent));
		actionbar.setTitle(getResources().getString(R.string.newposttitle));
		
		progressdialog = new ProgressDialog(this);
		progressdialog.setCancelable(false);
		progressdialog.setCanceledOnTouchOutside(false);
		progressdialog.setProgressStyle(progressdialog.STYLE_SPINNER);		
		
		builder = new AlertDialog.Builder(this);
    	builder.setIcon(R.drawable.as1).setTitle(R.string.newpostfailed);	
    	builder.setPositiveButton("OK", null);
    	
    	attachmenturlarraylist = new ArrayList<String>();
    	
		Bundle bundle = getIntent().getExtras();
		bid = bundle.getString("bid");
		fid = bundle.getString("fid");
		board = bundle.getString("board");
		TableRow tablerow = (TableRow) findViewById(R.id.tableRow1);
		tablerow.setVisibility(View.GONE);
		NewTopicTitle = (EditText) findViewById(R.id.newtopictitle);
		ETEditText = (EditText) findViewById(R.id.newpostedittext);
		imagelayout = (LinearLayout)findViewById(R.id.imagelayout);
		ButtonUpload = (Button) findViewById(R.id.uploadattachmentbutton);
		ButtonNewpost = (Button) findViewById(R.id.newpostbutton);
		OnClickListener listener = new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				switch(v.getId()){
				
				// to upload an attachment
				case R.id.uploadattachmentbutton:
    				Intent intent = new Intent();
    				intent.setAction(Intent.ACTION_GET_CONTENT);
    				intent.setType("image/*");
    				Log.v(TAG, "startActivityForResult");
    				startActivityForResult(intent, 1);
    				break;
    				
    			// to post a new topic
				case R.id.newpostbutton:
					if(NewTopicTitle.getText().toString().trim().equals("")){
		            	builder.setMessage(getResources().getString(R.string.titlenotnull));
		            	builder.show();						
					}else{
    					asynctask = new ReplypostAsyncTask();
    					asynctask.execute();			
    					break;
    				}
				}
			}
			
		};

		ButtonUpload.setOnClickListener(listener);
		ButtonNewpost.setOnClickListener(listener);
		getnewpostasynctask = new GetReplyPostPageAsyncTask();
		getnewpostasynctask.execute();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
//		return super.onOptionsItemSelected(item);
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
		return true;
	}	
	
	@Override
	protected void onActivityResult(int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub
		Log.v("####image", String.valueOf(requestCode)+String.valueOf(resultCode));
        if (resultCode == RESULT_OK) {  
            Uri uri = data.getData();  
            Log.v("uri", uri.toString());  

            ContentResolver cr = this.getContentResolver();  
            String [] proj={MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DISPLAY_NAME,
            		MediaStore.Images.Media.MIME_TYPE};  
            Cursor cursor = cr.query(uri, proj, null, null, null);
            int sizeindex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int intfilepath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int intdisplayname = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int intmimetype = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
            cursor.moveToFirst();
            long size = cursor.getLong(sizeindex);
            String filepath = cursor.getString(intfilepath);
            String filename = cursor.getString(intdisplayname);
            String mimetype = cursor.getString(intmimetype);
            cursor.close();
            Log.v(TAG, Long.toString(size));
            Log.v(TAG, "file path is "+filepath);
            if(size>=attachmentmaxsize){

            	builder.setMessage(getResources().getString(R.string.attachementsizeerror1)+attachmentmaxsize/1024 + getResources().getString(R.string.attachementsizeerror2));
            	builder.show();
            	
            }else{
            	UploadAttachmentAsyncTask uploadattachmentasynctask = new UploadAttachmentAsyncTask();
            	uploadattachmentasynctask.execute(filepath, filename, mimetype);

            }
        } 
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	protected class GetReplyPostPageAsyncTask extends AsyncTask<Object, Object, Object>{
    	private HashMap<String, String> cookie;
    	private FudanBBSApplication currentapplication;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressdialog.setMessage(getString(R.string.generatenewpostpage));
			progressdialog.show();		
			currentapplication = (FudanBBSApplication)getApplication();
			cookie = new  HashMap<String, String>();
			cookie = currentapplication.get_cookie();	
			attachmentenability = "0";
			Log.v(TAG, "onPreExecute");
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Object result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(attachmentmaxsize>0){
				Log.v(TAG, "attachment enabled");
			}else{
				ButtonUpload.setVisibility(View.GONE);
				Log.v(TAG, "attachment disabled");				
			}
			if(false == title.isEmpty()){
				NewTopicTitle.setText(title);
				NewTopicTitle.clearFocus();
			}
			if(false == replytext.isEmpty()){
				ETEditText.setText("\n\n"+replytext);
				ETEditText.setSelection(0);
				ETEditText.requestFocus();
				ETEditText.setFocusable(true);
				
			}
			if(progressdialog.isShowing()){
				progressdialog.dismiss();
			}
			Log.v(TAG, "onPostExecute");			
		}

		@Override
		protected Object doInBackground(Object... params) {
			// TODO Auto-generated method stub
			String url = "http://bbs.fudan.edu.cn/bbs/con?new=1&bid="+bid+"&f="+fid;
			Log.v(TAG, url);
			Document doc;
			try {
				if(cookie.get("utmpuserid")!=null){
					doc = Jsoup.connect(url).cookies(cookie).get();
				}else{
					doc = Jsoup.connect(url).get();					
				}	
				Elements elements = doc.getElementsByTag("bbscon");
				for(Element element : elements){

					attachmentmaxsize = (Integer.valueOf(element.attr("attach").toString().trim()));
					Log.v(TAG, "max attachment size is "+attachmentmaxsize);
				}
				elements = doc.getElementsByTag("po");
				for(Element element : elements){
					for(Element element3: element.getAllElements()){
//    					Log.v(TAG, element3.html());
//    					Log.v(TAG, "tag name is "+element3.tagName());
//    					Log.v(TAG, "tag attr is "+element3.attr("m").toString());
    					if("owner".equals(element3.tagName())){
    						owner = element3.text().toString().trim();
    					}
    					if("nick".equals(element3.tagName())){
    						nick = element3.text().toString().trim();
    					}
    					if("title".equals(element3.tagName())){
    						title = element3.text().toString().trim();
    						if(false == title.contains("Re")){
    							title = "Re: "+title;
    						}
    					}
//    					Log.v(TAG, "owner is "+owner+" nick is "+nick+" title is "+title);
//    					replytext = "<pa m='q'>"+replytext; 
    					replytext = getString(R.string.replytext1)+owner+getString(R.string.replytext2)+"\n";
					}
					Elements elements2 = doc.getElementsByAttributeValue("m", "t");

						for(Element element4: elements2){
							for(Element element5: element4.getElementsByTag("p")){
								String content = element5.text().toString().trim();
								Log.v(TAG, "content is "+content);
								if(!content.isEmpty()){
									replytext = replytext +":"+ element5.text().toString()+"\n";												
								}
					
							}
						}
//				replytext = replytext + "</pa>";		
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
	}
	
 	protected class ReplypostAsyncTask extends AsyncTask<Object, Object, Object>{
    	private HashMap<String, String> cookie;
    	private StringBuffer cookiestring;
    	private FudanBBSApplication currentapplication;		
    	private HttpURLConnection con;
    	private String topictitle, topictext, topiccontent;
    	private int responsecode;
    	private String responsemessage;
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressdialog.setMessage(getString(R.string.newposting));
			progressdialog.show();		
			currentapplication = (FudanBBSApplication)getApplication();
			cookie = new  HashMap<String, String>();
			cookie = currentapplication.get_cookie();	
			cookiestring = new StringBuffer();
			for(String key :cookie.keySet()){
				cookiestring.append(key).append("=").append(cookie.get(key)).append(";");
			}
			cookiestring.deleteCharAt(cookiestring.length()-1);
			topictitle = NewTopicTitle.getText().toString().trim();
			topictext = ETEditText.getText().toString().trim()+"\n";
			if(attachmenturlarraylist.size()>0){
				for(String attachment : attachmenturlarraylist){
					topictext = "\n"+topictext + attachment + "\n";
					Log.v(TAG, "attachment url is"+attachment);
				}
			}
			topictext = topictext+"\n\n"+getResources().getString(R.string.signature);
			Log.v(TAG, "onPreExecute");
			super.onPreExecute();

		}

		@Override
		protected void onPostExecute(Object result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(200 == responsecode && responsemessage.equals("OK")){
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.newpostsuccess), Toast.LENGTH_SHORT).show();
				finish();
			}
			if(progressdialog.isShowing()){
				progressdialog.dismiss();
			}
			Log.v(TAG, "onPostExecute");
		}

		@Override
		protected Object doInBackground(Object... params) {
			// TODO Auto-generated method stub
			try {
				URL url = new URL("http://bbs.fudan.edu.cn/bbs/snd?bid="+bid+"&f="+fid+"&utf8=1");
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("POST");
				con.setConnectTimeout(15000);
  	          	con.setDoInput(true);
  	          	con.setDoOutput(true);
  	          	con.setUseCaches(false);
				con.setRequestProperty("Cookie", cookiestring.toString());
				con.setRequestProperty("Charset", "utf-8");
				con.setRequestProperty("Referer", "http://bbs.fudan.edu.cn/bbs/con?new=1&bid="+bid+"&f="+fid);
				con.setRequestProperty("Connection", "keep-alive");
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
				con.connect();
				DataOutputStream ds = new DataOutputStream(con.getOutputStream());
				StringBuffer content = new StringBuffer("title="+topictitle+"&sig=1&text="+topictext);


				Log.v(TAG, content.toString());
				ds.write(content.toString().getBytes("utf-8"));

				ds.flush();
    	        ds.close();
    	        responsecode= con.getResponseCode();
    	        responsemessage = con.getResponseMessage();
    	        con.disconnect();

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
			
			return null;
		}
		
	}
	
	
	protected class UploadAttachmentAsyncTask extends AsyncTask<Object, Object, Object>{
		private String filepath, filename, mimetype;
		private int responsecode;
		private String responsemessage;
		private String attachmenturl;
    	private HashMap<String, String> cookie;
    	private StringBuffer cookiestring;
    	private FudanBBSApplication currentapplication;		
    	private HttpURLConnection con;
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			progressdialog.setMessage(getString(R.string.uploadingattachment));
			progressdialog.show();		
			currentapplication = (FudanBBSApplication)getApplication();
			cookie = new  HashMap<String, String>();
			cookie = currentapplication.get_cookie();	
			cookiestring = new StringBuffer();
			for(String key :cookie.keySet()){
				cookiestring.append(key).append("=").append(cookie.get(key)).append(";");
			}
			cookiestring.deleteCharAt(cookiestring.length()-1);
			Log.v(TAG, "cookiestring is "+cookiestring);

			Log.v(TAG, "onPreExecute");

		}

		@Override
		protected void onPostExecute(Object result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(progressdialog.isShowing()){
				progressdialog.dismiss();
			}
	          if(null != attachmenturl){
	        	  attachmenturlarraylist.add(attachmenturl);
	        	  Toast.makeText(getApplicationContext(), getString(R.string.uploadattachmentsuccess), Toast.LENGTH_SHORT).show();
                    try {  
                        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(filepath));  
                        ImageView imageview = new ImageView(getApplicationContext());
                        imageview.setImageBitmap(bitmap);  
            			imagelayout.addView(imageview);
    
                    } catch (FileNotFoundException e) {  
                        Log.e("Exception", e.getMessage(),e);  
                    }  
			}else{
				Toast.makeText(getApplicationContext(), R.string.uploadattachmentfail, Toast.LENGTH_SHORT).show();
			}

			Log.v(TAG, "onPostExecute");
		}

		@Override
		protected Object doInBackground(Object... params) {
			// TODO Auto-generated method stub
			Log.v(TAG, "doInBackground start");
			filepath =  (String) params[0];
			filename = (String) params[1];
			mimetype = (String) params[2];
			try {
				URL url = new URL("http://bbs.fudan.edu.cn/bbs/upload?b="+board);
				Log.v(TAG, url.toString());
		        String end ="\r\n";
		        String twoHyphens ="--";
		        String boundary ="---------------------------253522105810294";
    	        con=(HttpURLConnection)url.openConnection();

    	         con.setConnectTimeout(20000);
    	          con.setDoInput(true);
    	          con.setDoOutput(true);
    	          con.setUseCaches(false);
    	          con.setRequestProperty("Cookie", cookiestring.toString());

    	          con.setRequestMethod("POST");
    	          /* setRequestProperty */
    	          con.setRequestProperty("Connection", "Keep-Alive");
//    	          con.setRequestProperty("Charset", "UTF-8");
    	          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0");
    	          con.setRequestProperty("Referer", "http://bbs.fudan.edu.cn/bbs/preupload?board="+board);
    	          con.setRequestProperty("Content-Type",
    	                             "multipart/form-data;boundary="+boundary);

    	          con.connect();

		          DataOutputStream ds =
				            new DataOutputStream(con.getOutputStream());
				  ds.writeBytes(twoHyphens + boundary + end);
		          ds.writeBytes("Content-Disposition: form-data; name=\"up\"; filename=\""+filename+"\""+ end);
		          ds.writeBytes("Content-Type: "+mimetype+end);
		          ds.writeBytes(end);  

		          FileInputStream fStream =new FileInputStream(filepath);

		          int bufferSize =1024;
		          byte[] buffer =new byte[bufferSize];
		          int length =-1;

		          while((length = fStream.read(buffer)) !=-1)
		          {

		            ds.write(buffer, 0, length);
		          }
		          ds.writeBytes(end);
		          ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
		          /* close streams */
		          fStream.close();
		          ds.flush();


		          responsecode = con.getResponseCode();
		          responsemessage = con.getResponseMessage();
		          Log.v(TAG, "responsemessage is "+responsemessage);

		          ds.close();
				if(200 == responsecode){		          
		          InputStream is = con.getInputStream();
		          BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));  
		          StringBuilder response = new StringBuilder();  
		          String oneLine;  
		          String lineEnd = System.getProperty("line.separator"); 
		          while((oneLine = input.readLine()) != null) {  
		                response.append(oneLine + lineEnd);  
		            }  
		          if(null != response){
		        	  try{
		        		  attachmenturl = response.substring(response.indexOf("<url>")+5, response.indexOf("</url>"));
		        	  }catch(Exception e){
		        		  e.printStackTrace();
		        	  }
		        	  Log.v(TAG, "attachment url is "+attachmenturl);
		          }

//		          Log.v(TAG, "response content is "+response);
				}



				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					con.disconnect();
				}
				
			Log.v(TAG, "doInBackground end");
			return null;
		}
	}
}
