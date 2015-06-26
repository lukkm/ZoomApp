package zoomapp.facebook.com.zoomapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.facebook.FacebookSdk;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainActivity extends ActionBarActivity {

  private Button mLoadFromLocalButton;
  private ImageView mImageView;
  private Bitmap mImageBitmap;
  private Button mSendButton;
  private Button mCameraButton;
  private View mBox;
  private String mGifName;
  private Uri mCameraImageUri;
  private View mWaitOverlay;
  private float x;
  private float y;

  private static int SELECT_PICTURE = 1;
  private static int CAMERA_REQUEST = 2;

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
    if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
      Uri selectedImage = mCameraImageUri;
      getContentResolver().notifyChange(selectedImage, null);
      ContentResolver cr = getContentResolver();
      try {
        mImageBitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);

        mImageView.setVisibility(View.VISIBLE);
        mImageView.setImageBitmap(mImageBitmap);
      } catch (Exception e) {
       // Empty
      }

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
    mSendButton = (Button) findViewById(R.id.send_button);
    mBox = findViewById(R.id.box);
    mCameraButton = (Button) findViewById(R.id.get_image_from_camera);
    mWaitOverlay = findViewById(R.id.wait);
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

        x = event.getAxisValue(MotionEvent.AXIS_X);
        y = event.getAxisValue(MotionEvent.AXIS_Y);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        if (mImageBitmap.getWidth() > mImageBitmap.getHeight()) {
          x = x * mImageBitmap.getWidth() / width;
          y = y * mImageBitmap.getWidth() / width;
        } else {
          x = x * mImageBitmap.getHeight() / height;
          y = y * mImageBitmap.getHeight() / height;
        }




        mBox.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(200, 200);
        if (mImageBitmap.getWidth() > mImageBitmap.getHeight()) {
          params.leftMargin = (int) Math.max(x - 200 * ((float) mImageBitmap.getWidth() / width), 0);
          params.topMargin = (int) Math.max(y - 200 * ((float) mImageBitmap.getWidth() / width), 0);
        } else {
          params.leftMargin = (int) Math.max(x, 0);
          params.topMargin = (int) Math.max(y, 0);
        }
        mBox.setLayoutParams(params);

        mSendButton.setVisibility(View.VISIBLE);
        return false;
      }
    });

    mCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(),  "temp.jpg");
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
            Uri.fromFile(photo));
        mCameraImageUri = Uri.fromFile(photo);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
      }
    });

    mSendButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mWaitOverlay.setVisibility(View.VISIBLE);
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
        gifEncoder.setQuality(20);

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
        bitmap = createBitmap(mImageBitmap, factor, (int) x, (int) y);
        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

        factor = 6;
        bitmap = createBitmap(mImageBitmap, factor, (int) x, (int) y);
        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

        factor = 10;
        bitmap = createBitmap(mImageBitmap, factor, (int) x, (int) y);
        gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));
        gifEncoder.finish();

        try {
          if (fileos != null) {
            fileos.close();
          }
        } catch (IOException e) {
          Logger.getLogger("Holis").log(Level.INFO, "FUCK YOU");
        }

        mGifName = gifName;
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(gifName)));

        ShareToMessengerParams shareToMessengerParams =
            ShareToMessengerParams.newBuilder(Uri.fromFile(new File(mGifName)), "image/gif").build();
        MessengerUtils.shareToMessenger(
            MainActivity.this,
            1,
            shareToMessengerParams);
        MessengerUtils.finishShareToMessenger(MainActivity.this, shareToMessengerParams);
      }
    });
  }

  private Bitmap createBitmap(Bitmap originalBitmap, int factor, int x, int y) {
    return Bitmap.createBitmap(originalBitmap,
        Math.max(x - originalBitmap.getWidth()/factor, 0),
        Math.max(y - originalBitmap.getWidth()/factor, 0),
        originalBitmap.getWidth()/factor + Math.min(originalBitmap.getWidth()/factor, originalBitmap.getWidth() - x),
        originalBitmap.getWidth()/factor + Math.min(originalBitmap.getWidth()/factor, originalBitmap.getHeight() - y));
  }
}
