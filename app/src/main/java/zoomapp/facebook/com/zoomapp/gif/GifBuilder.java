package zoomapp.facebook.com.zoomapp.gif;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import zoomapp.facebook.com.zoomapp.AnimatedGifEncoder;

/**
 * Class to create a gif based on an image and the coordinates
 */
public abstract class GifBuilder extends AsyncTask<Integer, Integer, String> {

  private final Bitmap mImageBitmap;
  private final String mGifName;
  private final Context mContext;

  public GifBuilder(Context context, Bitmap imageBitmap, String gifName) {
    mContext = context;
    mImageBitmap = imageBitmap;
    mGifName = gifName;
  }

  protected String doInBackground(Integer... params) {
    if (params.length < 2) {
      return null;
    }
    int x = params[0];
    int y = params[1];
    AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();

    //String gifName = Environment.getExternalStorageDirectory()
    //    + File.separator + "testgif.gif";

    FileOutputStream fileos = null;
    try {
      fileos = new FileOutputStream(mGifName);
      gifEncoder.start(fileos);
    } catch (IOException e) {
      Logger.getLogger("ZoomApp").log(Level.INFO, "Error on IO GIFFFFF");
    }

    gifEncoder.setRepeat(0);
    gifEncoder.setDelay(1000);
    gifEncoder.setQuality(20);

    int factor = 2;
    Bitmap bitmap;

    if (mImageBitmap.getWidth() > mImageBitmap.getHeight()) {
      bitmap = Bitmap.createBitmap(mImageBitmap,
          Math.max(x - mImageBitmap.getHeight() / factor, 0),
          0,
          mImageBitmap.getHeight() / factor + Math.min(
              mImageBitmap.getHeight() / factor, mImageBitmap.getWidth() - x),
          mImageBitmap.getHeight());
    } else {
      bitmap = Bitmap.createBitmap(mImageBitmap,
          0,
          Math.max(y - mImageBitmap.getWidth() / factor, 0),
          mImageBitmap.getWidth(),
          mImageBitmap.getWidth() / factor + Math.min(
              mImageBitmap.getWidth() / factor, mImageBitmap.getHeight() - y));
    }

    int firstWidth = bitmap.getWidth();
    int firstHeight = bitmap.getHeight();

    gifEncoder.addFrame(bitmap);

    bitmap = createBitmap(mImageBitmap, 4, x, y);
    gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

    bitmap = createBitmap(mImageBitmap, 6, x, y);
    gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));

    bitmap = createBitmap(mImageBitmap, 10, x, y);
    gifEncoder.addFrame(Bitmap.createScaledBitmap(bitmap, firstWidth, firstHeight, false));
    gifEncoder.finish();

    try {
      if (fileos != null) {
        fileos.close();
      }
    } catch (IOException e) {
      Logger.getLogger("ZoomApp").log(Level.INFO, "Unable to finish decoding gif");
    }

    onDecodeFinished(mGifName);
    mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(mGifName)));
    return mGifName;
  }

  private Bitmap createBitmap(Bitmap originalBitmap, int factor, int x, int y) {
    int maxRelativeDimension =
        Math.max(originalBitmap.getWidth(), originalBitmap.getHeight()) / factor;
    return Bitmap.createBitmap(originalBitmap,
        Math.max(x - maxRelativeDimension, 0),
        Math.max(y - maxRelativeDimension, 0),
        maxRelativeDimension + Math.min(maxRelativeDimension, originalBitmap.getWidth() - x),
        maxRelativeDimension + Math.min(maxRelativeDimension, originalBitmap.getHeight() - y));
  }

  public abstract void onDecodeFinished(String gifName);

}
