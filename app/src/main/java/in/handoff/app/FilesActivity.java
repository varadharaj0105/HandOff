package in.handoff.app;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
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
 void work(Job job){ progress.setVisibility(View.VISIBLE);status.setText("Working…");box.setEnabled(false);for(int i=0;i<form.getChildCount();i++)form.getChildAt(i).setEnabled(false); files.setEnabled(false); for(int i=0;i<box.getChildCount();i++)box.getChildAt(i).setEnabled(false);
 worker.execute(()->{try{job.run();}catch(Exception e){message("Could not finish: "+e.getMessage());}finally{runOnUiThread(()->{progress.setVisibility(View.GONE);for(int i=0;i<form.getChildCount();i++)form.getChildAt(i).setEnabled(true);box.setEnabled(true);for(int i=0;i<box.getChildCount();i++)box.getChildAt(i).setEnabled(true);});}});}
 void message(String value){runOnUiThread(()->status.setText(value));}
 final int ink=0xff152c35, teal=0xff006b64, paper=0xfff4f5ef;
 LinearLayout form, hero;
 ScrollView scroll;
 Button send;
 TextView location;
 ProgressBar progress;
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 GradientDrawable bg(int color,int radius){
    GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 TextView label(String value,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
    t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",0));return t;
 }
 void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
 EditText field(String hint){
    TextView caption=label(hint,13,ink,true);form.addView(caption);
    EditText e=new EditText(this);e.setSingleLine();e.setTextSize(16);e.setTextColor(ink);
    e.setContentDescription(hint);e.setBackground(bg(0xffe8eeea,10));pad(e,12,8);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.setMargins(0,dp(4),0,dp(12));form.addView(e,lp);return e;
 }
 Button action(LinearLayout parent,String title,Runnable callback){
    Button b=new Button(this);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(-1);b.setText(title);
    b.setBackground(bg(teal,14));parent.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));b.setOnClickListener(v->callback.run());return b;
 }
 class LinkArt extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    LinkArt(){super(FilesActivity.this);}
    protected void onDraw(Canvas c){
       c.save();float scale=Math.min(getWidth()/110f,getHeight()/82f);c.scale(scale,scale);
       c.rotate(-7,50,40);p.setColor(0xff87d8ae);c.drawRoundRect(9,13,84,62,6,6,p);
       p.setColor(ink);c.drawRoundRect(14,18,79,55,3,3,p);
       p.setColor(0xff87d8ae);c.drawRoundRect(2,65,90,69,2,2,p);
       p.setColor(0xfff6cd76);c.drawRoundRect(68,31,97,76,5,5,p);
       p.setColor(ink);c.drawRoundRect(72,35,93,68,2,2,p);
       p.setColor(0xfff6cd76);c.drawCircle(31,36,3,p);c.drawCircle(42,36,3,p);c.drawCircle(53,36,3,p);c.restore();
    }
 }
 public void onCreate(Bundle state){super.onCreate(state);
    getWindow().setNavigationBarColor(paper);
    box=new LinearLayout(this);box.setOrientation(1);box.setBackgroundColor(paper);pad(box,18,8);setContentView(box);
    box.setOnApplyWindowInsetsListener((v,in)->{box.setPadding(dp(18),dp(8)+in.getSystemWindowInsetTop(),dp(18),dp(8)+in.getSystemWindowInsetBottom());return in;});
    hero=new LinearLayout(this);hero.setGravity(Gravity.CENTER_VERTICAL);hero.setBackground(bg(ink,22));pad(hero,18,12);box.addView(hero);
    TextView brand=label("HandOff",34,-1,true);hero.addView(brand,new LinearLayout.LayoutParams(0,-2,1));hero.addView(new LinkArt(),dp(94),dp(68));
    status=label("Ready to connect",14,teal,true);pad(status,4,8);box.addView(status);
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);box.addView(progress,-1,dp(3));progress.setVisibility(View.GONE);
    scroll=new ScrollView(this);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    form=new LinearLayout(this);form.setOrientation(1);form.setBackground(bg(-1,20));pad(form,16,16);scroll.addView(form);
    TextView title=label("Connect to laptop",24,ink,true);form.addView(title);
    TextView tip=label("Enter the details shown by your laptop launcher.",14,0xff4c6261,false);tip.setPadding(0,dp(4),0,dp(14));form.addView(tip);
 host=field("Laptop address");user=field("Username");password=field("Password");password.setInputType(129);pin=field("Laptop SHA256 host fingerprint");
 user.setText("handoff"); action(form,"Connect to laptop",()->{String h=host.getText().toString().trim(),u=user.getText().toString(),p=password.getText().toString(),f=pin.getText().toString().trim();
 work(()->{if(session!=null)session.disconnect();JSch j=new JSch();session=j.getSession(u,h,2022);session.setPassword(p);
 j.setHostKeyRepository(new HostKeyRepository(){public int check(String name,byte[] key){try{return fingerprint(key).equals(f)?OK:CHANGED;}catch(Exception e){return CHANGED;}}
 public void add(HostKey k,UserInfo i){}public void remove(String h,String t){}public void remove(String h,String t,byte[] k){}public String getKnownHostsRepositoryID(){return "Laptop";}public HostKey[] getHostKey(){return new HostKey[0];}public HostKey[] getHostKey(String h,String t){return getHostKey();}});
 session.setConfig("StrictHostKeyChecking","yes");session.connect(15000);sftp=(ChannelSftp)session.openChannel("sftp");sftp.connect(15000);refresh();});});
 location=label("",21,ink,true);pad(location,4,8);box.addView(location);location.setVisibility(View.GONE);
 send=action(box,"Send from phone",()->{if(sftp!=null&&sftp.isConnected())chooseMode(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*"),1));});
 send.setVisibility(View.GONE);files=new ListView(this);files.setBackground(bg(-1,16));files.setDividerHeight(dp(1));box.addView(files,new LinearLayout.LayoutParams(-1,0,1));files.setVisibility(View.GONE);}
 String fingerprint(byte[] key)throws Exception{return "SHA256:"+Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(key));}
 void chooseMode(Runnable next){new AlertDialog.Builder(this).setItems(new String[]{"Copy","Move after verification"},(d,n)->{moving=n==1;next.run();}).show();}
 void refresh()throws Exception{ArrayList<String> names=new ArrayList<>();for(Object obj:sftp.ls(".")){ChannelSftp.LsEntry e=(ChannelSftp.LsEntry)obj;String n=e.getFilename();if(n.equals(".")||e.getAttrs().isLink()||n.matches(".*[?*\\\\].*"))continue;names.add(n+(e.getAttrs().isDir()?"/":""));}Collections.sort(names);String path=sftp.pwd();
 runOnUiThread(()->{scroll.setVisibility(View.GONE);send.setVisibility(View.VISIBLE);files.setVisibility(View.VISIBLE);location.setVisibility(View.VISIBLE);
 status.setText("Connected  ·  "+host.getText());location.setText(path.equals("/")?"Laptop files":path);
 files.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,names){
    public View getView(int position,View old,ViewGroup parent){
       String name=getItem(position);boolean folder=name.endsWith("/");LinearLayout row=new LinearLayout(FilesActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);pad(row,12,8);
       TextView icon=label(folder?"DIR":"FILE",11,folder?teal:ink,true);icon.setGravity(Gravity.CENTER);icon.setBackground(bg(folder?0xffd5ede0:0xfff9e9c5,9));row.addView(icon,dp(40),dp(40));
       TextView text=label(name.equals("../")?"Parent folder":name,16,ink,false);pad(text,12,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(label("›",24,teal,false));return row;
    }
 });files.setOnItemClickListener((a,v,i,id)->{String n=names.get(i);if(n.endsWith("/"))work(()->{sftp.cd(n);refresh();});else{selected=n;chooseMode(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,n),2));}});});}
 public void onBackPressed(){if(scroll.getVisibility()==View.GONE){scroll.setVisibility(View.VISIBLE);files.setVisibility(View.GONE);location.setVisibility(View.GONE);send.setVisibility(View.GONE);status.setText("Connection settings");}else super.onBackPressed();}
 byte[] stream(InputStream source,OutputStream dest)throws Exception{try(InputStream in=source;OutputStream out=dest){MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[131072];int n;while((n=in.read(buf))!=-1){md.update(buf,0,n);if(out!=null)out.write(buf,0,n);}return md.digest();}}
 protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;Uri uri=data.getData();work(()->{ContentResolver r=getContentResolver();if(req==1){String name;try(android.database.Cursor c=r.query(uri,new String[]{"_display_name"},null,null,null)){c.moveToFirst();name=c.getString(0);}if(name==null||name.contains("/")||name.matches(".*[?*\\\\].*")||name.equals(".")||name.equals(".."))throw new IOException("Unsupported name");
 String temp=".handoff-"+UUID.randomUUID();try{byte[] hash=stream(r.openInputStream(uri),sftp.put(temp));if(!Arrays.equals(hash,stream(sftp.get(temp),null)))throw new IOException("Verification failed");
 try{sftp.lstat(name);throw new IOException("Name already exists");}catch(SftpException e){if(e.id!=ChannelSftp.SSH_FX_NO_SUCH_FILE)throw e;}sftp.rename(temp,name);
 if(moving&&Arrays.equals(hash,stream(r.openInputStream(uri),null))&&!DocumentsContract.deleteDocument(r,uri))throw new IOException("Copied; original retained");}finally{try{sftp.rm(temp);}catch(Exception ignored){}}}
 else{byte[] hash=stream(sftp.get(selected),r.openOutputStream(uri,"wt"));if(!Arrays.equals(hash,stream(r.openInputStream(uri),null)))throw new IOException("Verification failed");if(moving&&Arrays.equals(hash,stream(sftp.get(selected),null)))sftp.rm(selected);}refresh();message("Transfer completed");});}
}
