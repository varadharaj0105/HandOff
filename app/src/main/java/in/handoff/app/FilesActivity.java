package in.handoff.app;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.*;
import com.jcraft.jsch.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class FilesActivity extends Activity {
 LinearLayout box; TextView status; EditText host,user,password,pin; ListView files;
 Session session; ChannelSftp sftp;
 final ExecutorService worker=Executors.newSingleThreadExecutor();
 String selected; boolean moving;
 interface Job { void run() throws Exception; }
 void work(Job job){ box.setEnabled(false); files.setEnabled(false); for(int i=0;i<box.getChildCount();i++)box.getChildAt(i).setEnabled(false);
 worker.execute(()->{try{job.run();}catch(Exception e){message("Could not finish: "+e.getMessage());}finally{runOnUiThread(()->{box.setEnabled(true);for(int i=0;i<box.getChildCount();i++)box.getChildAt(i).setEnabled(true);});}});}
 void message(String value){runOnUiThread(()->status.setText(value));}
 EditText field(String hint){EditText e=new EditText(this);e.setSingleLine();e.setHint(hint);box.addView(e);return e;}
 void button(String title,Runnable action){Button b=new Button(this);b.setText(title);box.addView(b);b.setOnClickListener(v->action.run());}
 public void onCreate(Bundle state){super.onCreate(state);box=new LinearLayout(this);box.setOrientation(1);box.setPadding(24,48,24,12);setContentView(box);
 status=new TextView(this);status.setText("HandOff  /  Laptop files");status.setTextSize(20);box.addView(status);
 host=field("Laptop address");user=field("Username");password=field("Password");password.setInputType(129);pin=field("Laptop SHA256 host fingerprint");
 button("Connect",()->{String h=host.getText().toString().trim(),u=user.getText().toString(),p=password.getText().toString(),f=pin.getText().toString().trim();
 work(()->{if(session!=null)session.disconnect();JSch j=new JSch();session=j.getSession(u,h,2022);session.setPassword(p);
 j.setHostKeyRepository(new HostKeyRepository(){public int check(String name,byte[] key){try{return fingerprint(key).equals(f)?OK:CHANGED;}catch(Exception e){return CHANGED;}}
 public void add(HostKey k,UserInfo i){}public void remove(String h,String t){}public void remove(String h,String t,byte[] k){}public String getKnownHostsRepositoryID(){return "Laptop";}public HostKey[] getHostKey(){return new HostKey[0];}public HostKey[] getHostKey(String h,String t){return getHostKey();}});
 session.setConfig("StrictHostKeyChecking","yes");session.connect(15000);sftp=(ChannelSftp)session.openChannel("sftp");sftp.connect(15000);refresh();});});
 button("Send from phone",()->{if(sftp!=null&&sftp.isConnected())chooseMode(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*"),1));});
 files=new ListView(this);box.addView(files);}
 String fingerprint(byte[] key)throws Exception{return "SHA256:"+Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(key));}
 void chooseMode(Runnable next){new AlertDialog.Builder(this).setItems(new String[]{"Copy","Move after verification"},(d,n)->{moving=n==1;next.run();}).show();}
 void refresh()throws Exception{ArrayList<String> names=new ArrayList<>();for(Object obj:sftp.ls(".")){ChannelSftp.LsEntry e=(ChannelSftp.LsEntry)obj;String n=e.getFilename();if(n.equals(".")||e.getAttrs().isLink()||n.matches(".*[?*\\\\].*"))continue;names.add(n+(e.getAttrs().isDir()?"/":""));}Collections.sort(names);String path=sftp.pwd();
 runOnUiThread(()->{status.setText(path);files.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names));files.setOnItemClickListener((a,v,i,id)->{String n=names.get(i);if(n.endsWith("/"))work(()->{sftp.cd(n);refresh();});else{selected=n;chooseMode(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,n),2));}});});}
 byte[] stream(InputStream source,OutputStream dest)throws Exception{try(InputStream in=source;OutputStream out=dest){MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[131072];int n;while((n=in.read(buf))!=-1){md.update(buf,0,n);if(out!=null)out.write(buf,0,n);}return md.digest();}}
 protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;Uri uri=data.getData();work(()->{ContentResolver r=getContentResolver();if(req==1){String name;try(android.database.Cursor c=r.query(uri,new String[]{"_display_name"},null,null,null)){c.moveToFirst();name=c.getString(0);}if(name==null||name.contains("/")||name.matches(".*[?*\\\\].*")||name.equals(".")||name.equals(".."))throw new IOException("Unsupported name");
 String temp=".handoff-"+UUID.randomUUID();try{byte[] hash=stream(r.openInputStream(uri),sftp.put(temp));if(!Arrays.equals(hash,stream(sftp.get(temp),null)))throw new IOException("Verification failed");
 try{sftp.lstat(name);throw new IOException("Name already exists");}catch(SftpException e){if(e.id!=ChannelSftp.SSH_FX_NO_SUCH_FILE)throw e;}sftp.rename(temp,name);
 if(moving&&Arrays.equals(hash,stream(r.openInputStream(uri),null))&&!DocumentsContract.deleteDocument(r,uri))throw new IOException("Copied; original retained");}finally{try{sftp.rm(temp);}catch(Exception ignored){}}}
 else{byte[] hash=stream(sftp.get(selected),r.openOutputStream(uri,"wt"));if(!Arrays.equals(hash,stream(r.openInputStream(uri),null)))throw new IOException("Verification failed");if(moving&&Arrays.equals(hash,stream(sftp.get(selected),null)))sftp.rm(selected);}refresh();message("Transfer completed");});}
}
