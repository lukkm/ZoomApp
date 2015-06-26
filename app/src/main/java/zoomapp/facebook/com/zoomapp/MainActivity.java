package zoomapp.facebook.com.zoomapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.facebook.FacebookSdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainActivity extends ActionBarActivity {

  private Button mLoadFromLocalButton;
  private ImageView mImageView;
  private Bitmap mImageBitmap;

  private static int SELECT_PICTURE = 1;
  private static int PIC_CROP = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext());

    setContentView(R.layout.activity_main);
    setUi();
    setListeners();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PICTURE && data != null) {
      Uri selectedImageUri = data.getData();
      String selectedImagePath = getPath(selectedImageUri);
      mImageView.setVisibility(View.VISIBLE);
      mImageBitmap = BitmapFactory.decodeFile(selectedImagePath);
      mImageView.setImageBitmap(mImageBitmap);
    }
  }

  private String getPath(Uri uri) {
    String[] projection = { MediaStore.Images.Media.DATA };
    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
    if (cursor == null) {
      return null;
    }
    int columnIndex = cursor.getColumnIndex(projection[0]);
    cursor.moveToFirst();
    String path = cursor.getString(columnIndex);
    cursor.close();
    return path;
  }

  private void setUi() {
    mImageView = (ImageView) findViewById(R.id.image_preview);
    mLoadFromLocalButton = (Button) findViewById(R.id.find_image_from_local_storage_button);
  }

  private void setListeners() {
    mLoadFromLocalButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        intent.setType("image/*");

        startActivityForResult(intent, SELECT_PICTURE);
      }
    });

    mImageView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        x = x * mImageBitmap.getWidth() / width;
        y = y * mImageBitmap.getWidth() / width;

        AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();

        String gifName = Environment.getExternalStorageDirectory()
            + File.separator + "testgif.gif";

        FileOutputStream fileos = null;
        try {
          fileos = new FileOutputStream(gifName);
          gifEncoder.start(fileos);
        } catch (IOException e) {
          Logger.getLogger("Holis").log(Level.INFO, "Error on IO GIFFFFF");
        }

        gifEncoder.setRepeat(0);
        gifEncoder.setDelay(1000);

        int factor = 2;

        Bitmap bitmap = null;
        if (mImageBitmap.getWidth() > mImageBitmap.getHeight()) {
          bitmap = Bitmap.createBitmap(mImageBitmap,
              Math.max((int) x - mImageBitmap.getHeight() / factor, 0),
              0,
              mImageBitmap.getHeight() / factor + (int) Math.min(mImageBitmap.getHeight() / factor, mImageBitmap.getWidth() - x),
              mImageBitmap.getHeight());
        } else {
          bitmap = Bitmap.createBitmap(mImageBitmap,
              0,
              Math.max((int) y - mImageBitmap.getWidth() / factor, 0),
              mImageBitmap.getWidth(),
              mImageBitmap.getWidth() / factor + (int) Math.min(mImageBitmap.getWidth() / factor, mImageBitmap.getHeight() - y));
        }

        int firstWidth = bitmap.getWidth();
        int firstHeight = bitmap.getHeight();

        gifEncoder.addFrame(bitmap);

        factor = 4;

        bitmap = Bitmap.createBitmap(mImageBitmap,
            Math.max((int) x - mImageBitmap.getWidth()/factor, 0),
            Math.max((int) y - mImageBitmap.getWidth()/factor, 0),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth()/factor, mImageBitmap.getWidth() - x),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth()/factor, mImageBitmap.getHeight() - y));

        mImageView.setImageBitmap(bitmap);

        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

        factor = 6;

        bitmap = Bitmap.createBitmap(mImageBitmap,
            Math.max((int) x - mImageBitmap.getWidth()/factor, 0),
            Math.max((int) y - mImageBitmap.getWidth()/factor, 0),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth()/factor, mImageBitmap.getWidth() - x),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth() / factor, mImageBitmap.getHeight() - y));

        storeImage(bitmap);
        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

        factor = 10;

        bitmap = Bitmap.createBitmap(mImageBitmap,
            Math.max((int) x - mImageBitmap.getWidth()/factor, 0),
            Math.max((int) y - mImageBitmap.getWidth()/factor, 0),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth()/factor, mImageBitmap.getWidth() - x),
            mImageBitmap.getWidth()/factor + (int) Math.min(mImageBitmap.getWidth()/factor, mImageBitmap.getHeight() - y));

        storeImage(bitmap);
        mImageView.setImageBitmap(bitmap);
        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

        gifEncoder.finish();

        try {
          if (fileos != null) {
            fileos.close();
          }
        } catch (IOException e) {
          Logger.getLogger("Holis").log(Level.INFO, "FUCK YOU");
        }

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(gifName)));

        Logger.getLogger("Holis").log(Level.INFO, "Axis: " + x + ", " + y);
        return false;
      }
    });
  }

  private void storeImage(Bitmap image) {

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    image.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

    //you can create a new file name "test.jpg" in sdcard folder.
    String fileName = Environment.getExternalStorageDirectory()
        + File.separator + image.hashCode() + ".jpg";
    File f = new File(fileName);
    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
    try {
      f.createNewFile();
      //write the bytes in file
      FileOutputStream fo = new FileOutputStream(f);
      fo.write(bytes.toByteArray());

      // remember close de FileOutput
      fo.close();
    } catch (IOException e){
      Logger.getLogger("Holis").log(Level.INFO, "Error on IO");
    }
/*
    File pictureFile = getOutputMediaFile();
    if (pictureFile == null) {
      return;
    }
    try {
      FileOutputStream fos = new FileOutputStream(pictureFile);
      image.compress(Bitmap.CompressFormat.PNG, 90, fos);
      fos.close();
      sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(pictureFile)));
    } catch (FileNotFoundException e) {
      Logger.getLogger("Holis").log(Level.INFO, "Error on file");
    } catch (IOException e) {

    }*/
  }

  /** Create a File for saving an image or video */
  private File getOutputMediaFile(){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
        + "/Android/data/"
        + getApplicationContext().getPackageName()
        + "/Files");

    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        Logger.getLogger("Holis").log(Level.INFO, "Error on mkdir");
        return null;
      }
    }
    // Create a media file name
    String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
    File mediaFile;
    String mImageName="MI_"+ timeStamp +".jpg";
    mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
    return mediaFile;
  }
}
